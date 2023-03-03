/*******************************************************************************
 * Copyright (C) 2021 Florian Sager, www.agitos.de
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.agitos.agiprx.bean.processor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.dao.DomainDao;
import de.agitos.agiprx.dao.ProjectDao;
import de.agitos.agiprx.dao.RelationType;
import de.agitos.agiprx.dns.DomainIpChecker;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.Backend;
import de.agitos.agiprx.model.Domain;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.output.HAProxyBackendFormatter;
import de.agitos.agiprx.util.Assert;

public class HAProxyProcessor extends AbstractProcessor implements DependencyInjector {

	private static HAProxyProcessor BEAN;

	// private static final Logger LOG =
	// Logger.getLogger(HAProxyProcessor.class.getName());

	private final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");

	private final static DateFormat DATE_FORMAT_YMD_HM = new SimpleDateFormat("yyyyMMdd-HHmm");
	static {
		DATE_FORMAT_YMD_HM.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public final static String CONFIG_PATH = "/etc/haproxy/";
	private final static String ARCHIVE_PATH = "/etc/haproxy/archive/";
	private final static String CERT_PATH = "/etc/haproxy/certs/";

	private final static String CONFIG_FILE = "haproxy.cfg";
	private final static String CONFIG_TMP_FILE = CONFIG_FILE + ".tmp";
	private final static String DOMAIN_TO_BCKE_FILE = "domain2backend.map";
	public final static String DOMAIN_TO_CERT_FILE = "domain2cert.map";
	private final static String DOMAIN_TO_REDIRECT_FILE = "domain2redirect.map";
	private final static String DOMAIN_TO_PREFIX_FILE = "domain2prefix.map";

	private final static String CONFIG_HEADER_FILE = "haproxy-header.cfg";
	private final static String CONFIG_FOOTER_FILE = "haproxy-footer.cfg";

	public final static String MAINTENANCE = "_maintenance";
	public final static String USERAUTH = "_userauth";

	private DomainIpChecker domainIpChecker;

	private HAProxyLesslBackendProcessor haproxyLesslBackendProcessor;

	private ProjectDao projectDao;

	private DomainDao domainDao;

	protected ConsoleWrapper console;

	private final String haProxyReloadCommand;

	// @Value("${cert.lesslissuerparname}")
	private final String lesslIssuerPartialName;

	public HAProxyProcessor() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		haProxyReloadCommand = Config.getBean().getString("haproxy.reloadCommand");

		lesslIssuerPartialName = Config.getBean().getString("cert.lesslissuerparname");
	}

	@Override
	public void postConstruct() {
		domainIpChecker = DomainIpChecker.getBean();
		haproxyLesslBackendProcessor = HAProxyLesslBackendProcessor.getBean();
		projectDao = ProjectDao.getBean();
		domainDao = DomainDao.getBean();
		console = ConsoleWrapper.getBean();
	}

	public static HAProxyProcessor getBean() {
		return BEAN;
	}

	public void manageConfiguration(boolean verbose, boolean archive)
			throws IOException, InterruptedException, AbortionException {

		String backupPrefix = "pre-" + DATE_FORMAT.format(new Date()) + "-";
		if (archive) {
			// save previous cfg/maps to pre-YYYYMMDD-HHMMSS-<filename>
			copyToArchive(CONFIG_FILE, backupPrefix);
			copyToArchive(DOMAIN_TO_BCKE_FILE, backupPrefix);
			copyToArchive(DOMAIN_TO_CERT_FILE, backupPrefix);
			copyToArchive(DOMAIN_TO_REDIRECT_FILE, backupPrefix);
			copyToArchive(DOMAIN_TO_PREFIX_FILE, backupPrefix);
		}

		// fetch all projects
		List<Project> allProjects = projectDao.findAllAsAdmin(EnumSet.of(RelationType.BACKEND, RelationType.DOMAIN,
				RelationType.CONTAINERREF, RelationType.CONTAINER));

		// generate haproxy.cfg
		generateAndCheckConfigFile(allProjects);

		// generate maps
		generateDomainToBackendAndRedirectMap(allProjects);
		generateDomainToCertMap(allProjects);

		// re-validate configuration
		validateConfig(CONFIG_FILE,
				"The new invalid configuration is already inplace so assure to fix the problem before reloading HAProxy!");

		// reload HAProxy
		exec(0, haProxyReloadCommand);

		if (verbose && archive) {
			compareWithArchive(CONFIG_FILE, backupPrefix);
			compareWithArchive(DOMAIN_TO_BCKE_FILE, backupPrefix);
			compareWithArchive(DOMAIN_TO_CERT_FILE, backupPrefix);
			compareWithArchive(DOMAIN_TO_REDIRECT_FILE, backupPrefix);
			compareWithArchive(DOMAIN_TO_PREFIX_FILE, backupPrefix);
		}

		console.printlnfStress("Reloaded HAProxy with new configuration.");
	}

	private void copyToArchive(String filename, String backupPrefix)
			throws IOException, InterruptedException, AbortionException {
		exec(0, "cp", "-p", CONFIG_PATH + filename, ARCHIVE_PATH + backupPrefix + filename);
	}

	private void moveCertToArchive(String certFilename, Date notValidAfterDate, boolean replaced)
			throws IOException, InterruptedException, AbortionException {
		String archivePrefix = DATE_FORMAT.format(new Date()) + (replaced ? "-replaced" : "") + "-cert-expires-"
				+ DATE_FORMAT.format(notValidAfterDate) + "-";
		exec(0, "mv", CERT_PATH + certFilename, ARCHIVE_PATH + archivePrefix + certFilename);
	}

	private void compareWithArchive(String filename, String backupPrefix) throws IOException, InterruptedException {
		StringBuilder output = new StringBuilder();
		StringBuilder errorOutput = new StringBuilder();

		exec(output, errorOutput, "diff", "-s", "--context=2", ARCHIVE_PATH + backupPrefix + filename,
				CONFIG_PATH + filename);

		console.printf("%s", output.toString());
		if (errorOutput.length() > 0) {
			console.printlnfError("%s", errorOutput.toString());
		}
	}

	private void generateAndCheckConfigFile(List<Project> allProjects)
			throws IOException, InterruptedException, AbortionException {

		StringBuilder buf = new StringBuilder();

		buf.append("#\n# DO NOT EDIT: this file is automatically generated by agiprx\n#\n\n");

		// append static header with sections global/default/frontend and default
		// backends
		buf.append(new String(Files.readAllBytes(Paths.get(CONFIG_PATH + CONFIG_HEADER_FILE))));
		buf.append("\n");

		// append letsencrypt-backend, dependent of master or slave instance
		// configuration
		haproxyLesslBackendProcessor.generateLesslBackend(buf);

		// append main config from agiprx db (backend sections only)
		HAProxyBackendFormatter backendFormatter = new HAProxyBackendFormatter();

		for (Project project : allProjects) {

			for (Backend backend : project.getBackends()) {

				// set back-reference
				backend.setProject(project);

				if (backend.isGlobalBackend()) {
					// skip 'nocontent' or domain redirect backend
					continue;
				}

				if (backend.getBackendContainers().size() == 0) {
					console.printlnfError("Project %s: backend %s cannot be created as container assignment is missing",
							project.getLabel(), backend.getLabel());
					continue;
				}

				backendFormatter.formatBackend(backend, buf);

				buf.append("\n");
			}
		}
		buf.append("\n");

		// append static footer (may be empty)
		buf.append(new String(Files.readAllBytes(Paths.get(CONFIG_PATH + CONFIG_FOOTER_FILE))));
		buf.append("\n");

		// write temp file
		Files.write(Paths.get(CONFIG_PATH + CONFIG_TMP_FILE), buf.toString().getBytes(), StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);

		// validate temp file
		validateConfig(CONFIG_TMP_FILE, "The old configuration is still active.");

		// if valid move to haproxy.cfg
		exec(0, "mv", CONFIG_PATH + CONFIG_TMP_FILE, CONFIG_PATH + CONFIG_FILE);
	}

	private void generateDomainToBackendAndRedirectMap(List<Project> allProjects)
			throws IOException, InterruptedException, AbortionException {

		StringBuilder backendBuf = new StringBuilder();
		StringBuilder redirectBuf = new StringBuilder();
		StringBuilder prefixBuf = new StringBuilder();

		backendBuf.append("#\n# DO NOT EDIT: this file is automatically generated by agiprx\n#\n\n");

		redirectBuf.append("#\n# DO NOT EDIT: this file is automatically generated by agiprx\n#\n\n");

		prefixBuf.append("#\n# DO NOT EDIT: this file is automatically generated by agiprx\n#\n\n");

		// append main config from agiprx db (backend sections only)
		for (Project project : allProjects) {
			for (Backend backend : project.getBackends()) {

				// set back-reference
				backend.setProject(project);

				backendBuf.append("# ").append(project.getLabel()).append(": ").append(backend.getLabel()).append("\n");

				for (Domain domain : backend.getDomainForwardings()) {
					if (backend.isGlobalBackend()) {

						backendBuf.append(domain.getDomain()).append(" ");

						boolean isTemporaryRedirect = Backend.TEMPORARY_REDIRECT_LABEL.equals(backend.getLabel());
						boolean isPermanentRedirect = Backend.PERMANENT_REDIRECT_LABEL.equals(backend.getLabel());

						if (isTemporaryRedirect || isPermanentRedirect) {

							// differ between location and prefix redirect by redirect URL syntax
							boolean isPrefixRedirect = domain.getRedirectToUrl() != null
									&& domain.getRedirectToUrl().endsWith(Domain.REDIRECT_KEEP_PATH_PATTERN);

							// use global label instead of FQ label (with IDs);
							if (isPrefixRedirect && isTemporaryRedirect) {
								backendBuf.append(Backend.TEMPORARY_PREFIX_LABEL);
								prefixBuf.append(domain.getDomain()).append(" ")
										.append(domain.getRedirectToUrlForMapping()).append("\n");
							} else if (isPrefixRedirect && isPermanentRedirect) {
								backendBuf.append(Backend.PERMANENT_PREFIX_LABEL);
								prefixBuf.append(domain.getDomain()).append(" ")
										.append(domain.getRedirectToUrlForMapping()).append("\n");
							} else {
								backendBuf.append(backend.getLabel());
								redirectBuf.append(domain.getDomain()).append(" ").append(domain.getRedirectToUrl())
										.append("\n");
							}

							backendBuf.append("\n");
						} else {
							// including 'nocontent'
							backendBuf.append(backend.getLabel()).append("\n");
						}

					} else {
						backendBuf.append(domain.getDomain()).append(" ").append(backend.getFQLabel()).append("\n");
					}
				}

				backendBuf.append("\n");
			}
		}

		// write to file
		Files.write(Paths.get(CONFIG_PATH + DOMAIN_TO_BCKE_FILE), backendBuf.toString().getBytes(),
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		Files.write(Paths.get(CONFIG_PATH + DOMAIN_TO_REDIRECT_FILE), redirectBuf.toString().getBytes(),
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		Files.write(Paths.get(CONFIG_PATH + DOMAIN_TO_PREFIX_FILE), prefixBuf.toString().getBytes(),
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	/**
	 * TODO: optimize map modification:
	 * https://www.haproxy.com/de/blog/introduction-to-haproxy-maps/ --> "Editing
	 * with http-request set-map"
	 */
	public void generateDomainToCertMap() throws IOException, InterruptedException, AbortionException {
		List<Project> allProjects = projectDao.findAllAsAdmin(EnumSet.of(RelationType.BACKEND, RelationType.DOMAIN));
		generateDomainToCertMap(allProjects);
	}

	private void generateDomainToCertMap(List<Project> allProjects)
			throws IOException, InterruptedException, AbortionException {

		// loop through CERT_PATH and find cert files for domains
		Map<String, CertInfo> domToCertFiles = fetchDomainToCertFiles(null);

		StringBuilder buf = new StringBuilder();

		buf.append("#\n# DO NOT EDIT: this file is automatically generated by agiprx\n#\n\n");

		// append main config from agiprx db (backend sections only)
		for (Project project : allProjects) {

			for (Backend backend : project.getBackends()) {

				buf.append("# ").append(project.getLabel()).append(": ").append(backend.getLabel()).append("\n");

				for (Domain domain : backend.getDomainForwardings()) {

					// set back-reference
					domain.setBackend(backend);

					String domainname = domain.getDomain();
					String wildcardDomain = null;
					int pointPos = domainname.indexOf(".");
					if (pointPos >= 0) {
						wildcardDomain = "*" + domainname.substring(pointPos);
					}

					if (domToCertFiles.containsKey(domainname)) {

						String certFilename = domToCertFiles.get(domainname).getFilename();

						buf.append(domainname).append(" ").append(certFilename).append("\n");

					} else if (wildcardDomain != null && domToCertFiles.containsKey(wildcardDomain)) {

						String certFilename = domToCertFiles.get(wildcardDomain).getFilename();

						buf.append(domainname).append(" ").append(certFilename).append("\n");

					} else if (Boolean.TRUE.equals(domain.getLetsEncrypt())) {

						// show an error on a non-existing LESSL cert only if the domainname resolves to
						// the proxy IP
						if (domainIpChecker.checkDomain(new ArrayList<String>(), domain.getDomain())) {
							console.printlnfError("No Let's Encrypt certificate available for %s", domain.getDomain());
						}

					} else if (Boolean.TRUE.equals(domain.getCertProvided())) {

						console.printlnfError("No certificate available for %s", domain.getDomain());
					}
				}

				buf.append("\n");
			}
		}

		// write to file
		Files.write(Paths.get(CONFIG_PATH + DOMAIN_TO_CERT_FILE), buf.toString().getBytes(), StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
	}

	/*
	 * This map is cached
	 */
	private Map<String, CertInfo> domainToCertFiles;

	public void resetDomainToCertFiles() {
		this.domainToCertFiles = null;
	}

	/*
	 * Generate the domain to CertInfo map detached, e.g. on daemon start or after a
	 * major change
	 */
	public void generateDomainToCertFilesDetached() {
		Runnable generator = () -> {
			resetDomainToCertFiles();
			fetchDomainToCertFiles(null);
		};
		new Thread(generator).start();
	}

	/*
	 * HINT: BE AWARE that domain names in certificates can be wildcards
	 */
	public Map<String, CertInfo> fetchDomainToCertFiles(List<String> warningMessages) {

		if (domainToCertFiles != null) {

			// LOG.fine("Use existing domain-to-cert-map " + domainToCertFiles.hashCode()+"
			// with "+domainToCertFiles.size()+" records.");

			return domainToCertFiles;
		}

		synchronized (this) {

			// another check for threads that waited for the synchronized block to finish
			if (domainToCertFiles == null) {

				Map<String, CertInfo> result = new HashMap<String, CertInfo>();

				File folder = new File(CERT_PATH);
				File[] listOfFiles = folder.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".pem");
					}

				});

				Set<String> lesslDomains = new HashSet<String>(domainDao.findAllLesslDomains());

				CertUsage certUsage = new CertUsage();

				for (File file : listOfFiles) {
					processCertFile(file, lesslDomains, result, warningMessages, certUsage);
				}

				this.domainToCertFiles = result;

				for (String certFilename : certUsage.getReplacedCertFilenames()) {
					CertInfo certInfo;
					try {
						certInfo = new CertInfo(certFilename);
						moveCertToArchive(certFilename, certInfo.getCert().getNotAfter(), true);
						warningMessages
								.add("Detected replaced cert " + certFilename + ", moved to archive " + ARCHIVE_PATH);
					} catch (Exception ioe) {
						warningMessages.add("Could not move replaced certificate " + certFilename + " to archive");
					}
				}

				// LOG.fine("Created new domain-to-cert-map " + domainToCertFiles.hashCode()+"
				// with "+domainToCertFiles.size()+" records.");
			}
		}

		return domainToCertFiles;
	}

	/**
	 * This methods adds a domain to CertInfo mapping to the result map if the map
	 * doesn't contain a valid domain mapping yet. If a certificate has expired the
	 * cert file is moved to the archive directory.
	 * 
	 * @param file         certificate File
	 * @param lesslDomains list of Letsencrypt domains from the database
	 * @param result       mapping file
	 */
	private void processCertFile(File file, Set<String> lesslDomains, Map<String, CertInfo> result,
			List<String> warningMessages, CertUsage certUsage) {

		if (file.isFile()) {
			try {
				CertInfo certInfo = new CertInfo(file.getName());

				if (!certInfo.isCurrentlyValid()) {
					if (certInfo.isExpired()) {
						// move this cert to archive
						moveCertToArchive(file.getName(), certInfo.getCert().getNotAfter(), false);
					}
					return;
				}

				if (certUsage != null) {
					certUsage.register(certInfo.getFilename());
				}

				for (String domainName : certInfo.getCoveredDomainnames()) {
					if (result.containsKey(domainName)) {

						CertInfo otherCertInfo = result.get(domainName);
						if (certInfo.hasPrecedence(lesslDomains.contains(domainName), otherCertInfo)) {
							result.put(domainName, certInfo);
							if (certUsage != null) {
								certUsage.increment(certInfo.getFilename());
								certUsage.decrement(otherCertInfo.getFilename());
							}
						} else if (certUsage != null) {
							certUsage.increment(otherCertInfo.getFilename());
							certUsage.decrement(certInfo.getFilename());
						}

					} else {
						result.put(domainName, certInfo);
						if (certUsage != null) {
							certUsage.increment(certInfo.getFilename());
						}
					}
				}
			} catch (Exception e) {
				String additionalInfo = "";
				if (e instanceof CertificateParsingException && "signed fields invalid".equals(e.getMessage())) {
					additionalInfo = ". Check the order of PEM sections: (1) certificate (2) opt. intermediate certs (3) private key.";
				}
				String msg = String.format("Exception %s with cert %s: %s%s", e.getClass().getSimpleName(),
						file.getName(), e.getMessage(), additionalInfo);
				if (warningMessages != null) {
					warningMessages.add(msg);
				}
				console.printlnfError(msg);
				return;
			}
		}
	}

	public void addLesslDomainToMap(Path combinedCert) {
		// Set<String> can be empty because new cert has precedence
		this.processCertFile(combinedCert.toFile(), new HashSet<String>(), this.fetchDomainToCertFiles(null), null,
				null);
	}

	public CertInfo getCertInfoForDomainName(String domainName) {

		Map<String, CertInfo> domainToCertFiles = fetchDomainToCertFiles(null);

		// return exact matches
		if (domainToCertFiles.containsKey(domainName)) {
			return domainToCertFiles.get(domainName);
		}

		int pointPos = domainName.indexOf(".");
		if (pointPos >= 0) {
			String wildcardDomain = "*" + domainName.substring(pointPos);
			return domainToCertFiles.get(wildcardDomain);
		}

		return null;
	}

	private void printDomainToCertList(Map<String, CertInfo> certMap, String domainFilter) {

		if (certMap.isEmpty()) {
			console.printlnfError("No currently valid certificates configured");
			return;
		}

		List<String> domains = new ArrayList<String>(certMap.keySet());
		Collections.sort(domains);

		if (domainFilter != null) {
			String pattern = domainFilter.replaceAll("\\?", ".?").replaceAll("\\*", ".*?");
			for (String domain : domains) {
				if (domain.matches(pattern)) {
					console.printlnf("%s : %s", domain, certMap.get(domain));
				}
			}
		} else {
			for (String domain : domains) {
				console.printlnf("%s : %s", domain, certMap.get(domain));
			}
		}
	}

	public class CertInfo {

		private String filename;
		private X509Certificate cert;

		public CertInfo(String filename) throws CertificateException, IOException {

			this.filename = filename;

			CertificateFactory fact = CertificateFactory.getInstance("X.509");
			FileInputStream is = new FileInputStream(CERT_PATH + filename);
			this.cert = (X509Certificate) fact.generateCertificate(is);
			is.close();
		}

		public String getFilename() {
			return filename;
		}

		public X509Certificate getCert() {
			return cert;
		}

		public boolean isCurrentlyValid() {
			try {
				this.cert.checkValidity();
			} catch (CertificateExpiredException | CertificateNotYetValidException e) {
				return false;
			}
			return true;
		}

		public boolean isExpired() {
			try {
				this.cert.checkValidity();
			} catch (CertificateExpiredException e) {
				return true;
			} catch (CertificateNotYetValidException e) {
				return false;
			}
			return false;
		}

		public boolean isValidInDays(int futureDays) {
			try {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DATE, futureDays);
				this.cert.checkValidity(cal.getTime());
			} catch (CertificateExpiredException | CertificateNotYetValidException e) {
				return false;
			}
			return true;
		}

		public boolean isNewerThan(X509Certificate otherCert) {
			return this.cert.getNotAfter().compareTo(otherCert.getNotAfter()) > 0;
		}

		public boolean hasPrecedence(boolean isLesslCert, CertInfo otherCertInfo) {
			if (isLesslCert) {
				if (this.isLesslCert() && !otherCertInfo.isLesslCert()) {
					return true;
				}
				if (!this.isLesslCert() && otherCertInfo.isLesslCert()) {
					return false;
				}
			}

			if (this.isNewerThan(otherCertInfo.getCert())) {
				return true;
			}
			return false;
		}

		public List<String> getCoveredDomainnames() throws CertificateParsingException {
			List<String> domains = new ArrayList<>();

			if (this.cert.getSubjectAlternativeNames() != null) {
				for (List<?> subjectAltName : this.cert.getSubjectAlternativeNames()) {
					for (Object item : subjectAltName) {
						if (item instanceof String) {
							domains.add((String) item);
						}
					}
				}
			}

			return domains;
		}

		public String getIssuerName() {
			return this.getCert().getIssuerX500Principal().getName();
		}

		public String getLesslIssuerDefaultPartialName() {
			return lesslIssuerPartialName;
		}

		public boolean isLesslCert() {
			return getIssuerName().contains(lesslIssuerPartialName);
		}

		@Override
		public String toString() {
			return "CertInfo:\n\tIssuerName = " + this.getIssuerName() + ",\n\tfilename = " + filename
					+ ",\n\tcert validity = " + DATE_FORMAT_YMD_HM.format(this.cert.getNotBefore()) + " - "
					+ DATE_FORMAT_YMD_HM.format(this.cert.getNotAfter()) + "]";
		}
	}

	private class CertUsage {

		Map<String, Integer> usage = new HashMap<String, Integer>();

		public void register(String filename) {
			if (!usage.containsKey(filename)) {
				usage.put(filename, null);
			}
		}

		public void increment(String filename) {
			Integer val = usage.get(filename);
			if (val == null) {
				usage.put(filename, 1);
			} else {
				usage.put(filename, val + 1);
			}
		}

		public void decrement(String filename) {
			Integer val = usage.get(filename);
			if (val == null) {
				usage.put(filename, -1);
			} else {
				usage.put(filename, val - 1);
			}
		}

		public List<String> getReplacedCertFilenames() {
			List<String> result = new ArrayList<String>();
			for (Map.Entry<String, Integer> entry : usage.entrySet()) {
				Integer val = entry.getValue();
				if (val != null && entry.getValue() <= 0) {
					// report unused certificate
					result.add(entry.getKey());
				}
			}
			return result;
		}
	}

	private void validateConfig(String filename, String abortMsgSuffix)
			throws AbortionException, IOException, InterruptedException {
		StringBuilder output = new StringBuilder();
		StringBuilder errorOutput = new StringBuilder();
		int exitCode = exec(output, errorOutput, "/usr/sbin/haproxy", "-c", "-V", "-f", CONFIG_PATH + filename);
		if (exitCode != 0) {
			console.printlnf(output.toString());
			console.printlnfError(errorOutput.toString());
			throw new AbortionException("Validation of " + CONFIG_PATH + filename + " failed with status code "
					+ exitCode + ". " + abortMsgSuffix);
		}
	}

	public List<String> getProductiveDomains() throws FileNotFoundException, IOException {

		List<String> result = new ArrayList<String>();

		File file = new File(CONFIG_PATH + DOMAIN_TO_BCKE_FILE);
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			for (String line; (line = br.readLine()) != null;) {
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] splitLine = line.split(" ");
				if (splitLine.length == 2) {
					result.add(splitLine[0]);
				}
			}
		}

		return result;
	}

	public void listCerts(String domainFilter) throws IOException, InterruptedException, AbortionException {
		Map<String, CertInfo> certMap = this.fetchDomainToCertFiles(null);
		printDomainToCertList(certMap, domainFilter);
	}

	public String getCertPath() {
		return CERT_PATH;
	}

	// public static Map<String, String> getProductiveDomainToCertMap() throws
	// FileNotFoundException, IOException {
	//
	// Map<String, String> result = new HashMap<String, String>();
	//
	// File file = new File(CONFIG_PATH + DOMAIN_TO_CERT_FILE);
	// try (BufferedReader br = new BufferedReader(new FileReader(file))) {
	// for (String line; (line = br.readLine()) != null;) {
	// String[] splitLine = line.split(" ");
	// if (splitLine.length == 2) {
	// result.put(splitLine[0], splitLine[1]);
	// }
	// }
	// }
	//
	// return result;
	// }
}
