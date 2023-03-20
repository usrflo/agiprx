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
package de.agitos.agiprx.executor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.bean.processor.HAProxyProcessor;
import de.agitos.agiprx.bean.processor.HAProxyProcessor.CertInfo;
import de.agitos.agiprx.dao.DomainDao;
import de.agitos.agiprx.dao.ProjectDao;
import de.agitos.agiprx.dto.DomainDto;
import de.agitos.agiprx.dto.DomainOperationDto;
import de.agitos.agiprx.dto.MassDomainUpdateDto;
import de.agitos.agiprx.model.Backend;
import de.agitos.agiprx.model.Domain;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.EmailSender;
import de.agitos.agiprx.util.UserContext;
import de.agitos.agiprx.util.Validator;

public class NonInteractiveDomainExecutor extends AbstractCertificateRelatedExecutor {

	private static NonInteractiveDomainExecutor BEAN;

	private static final Logger LOG = Logger.getLogger(NonInteractiveDomainExecutor.class.getName());

	private ProjectDao projectDao;

	private DomainDao domainDao;

	private HAProxyProcessor haProxyProcessor;

	private UserContext userContext;

	private ConsoleWrapper console;

	private Validator validator;

	private EmailSender emailSender;

	public static NonInteractiveDomainExecutor getBean() {
		return BEAN;
	}

	public NonInteractiveDomainExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		projectDao = ProjectDao.getBean();
		domainDao = DomainDao.getBean();
		haProxyProcessor = HAProxyProcessor.getBean();
		userContext = UserContext.getBean();
		console = ConsoleWrapper.getBean();
		validator = Validator.getBean();
		emailSender = EmailSender.getBean();
	}

	private Project checkAndFindProject(String projectLabel) {
		Project project = projectDao.find(projectLabel);
		if (project == null) {
			throw new RuntimeException("Invalid project: label " + projectLabel + " not existing or access denied");
		}
		return project;
	}

	private Backend checkAndFindBackend(Project project, String backendLabel) {

		for (Backend backend : project.getBackends()) {
			if (backend.getLabel().equals(backendLabel)) {
				return backend;
			}
		}

		throw new RuntimeException("Invalid backend: label " + backendLabel + " not existing or access denied");
	}

	/**
	 * Run mass updates of domain names of multiple projects/backends either in full
	 * synchronization mode or as partial operations.
	 * 
	 * @param massDomainUpdate
	 * @return List of unstructured warning messages
	 * @throws Exception
	 */
	public List<String> massDomainUpdate(MassDomainUpdateDto massDomainUpdate) throws Exception {

		if (!massDomainUpdate.isFullSync()) {

			// run single updates directly and return warnings
			return this.massDomainUpdateHelper(massDomainUpdate, null);

		} else {

			// start thread on full sync, run-detached with email information in the end
			Thread massUpdateThread = new Thread(
					new DetachedRunner(massDomainUpdate, userContext.getApiUser().getUsername()));
			massUpdateThread.start();

			// return nothing as no warnings are available yet
			return new ArrayList<String>();
		}
	}

	private List<String> massDomainUpdateHelper(MassDomainUpdateDto massDomainUpdate, String username)
			throws Exception {

		LOG.info("Starting mass domain update on " + massDomainUpdate.getDomainOperations().size() + " domain names.");

		PrintStream out = null;
		ByteArrayOutputStream bos = null;

		if (username != null) {
			userContext.registerApiUser(username);
		}

		if (massDomainUpdate.isFullSync()) {
			// there is nothing to input, so create dummy
			InputStream is = new ByteArrayInputStream(new byte[0]);
			bos = new ByteArrayOutputStream();
			out = new PrintStream(bos);
			console.registerClient(is, out, false);
		}

		List<String> warningMessages = new ArrayList<String>();

		Map<String, Project> projectMap = new HashMap<>();

		for (DomainOperationDto domainOp : massDomainUpdate.getDomainOperations()) {
			Project project = projectMap.get(domainOp.getProjectLabel());
			if (project == null) {
				project = checkAndFindProject(domainOp.getProjectLabel());
				projectMap.put(domainOp.getProjectLabel(), project);
			}
		}

		Set<String> containedDomains = massDomainUpdate.verifyUniq();

		boolean domainModification = false;

		for (DomainOperationDto domainOp : massDomainUpdate.getDomainOperations()) {
			Project project = projectMap.get(domainOp.getProjectLabel());
			Backend backend = checkAndFindBackend(project, domainOp.getBackendLabel());

			switch (domainOp.getOperation()) {
			case CREATE:
			case UPDATE:
			case REPLACE:
				domainModification |= createOrUpdateDomain(domainOp.getDomainName(), backend, domainOp.getEnableSSL(),
						domainOp.getRedirectToUrl(), warningMessages);
				LOG.info("Modified domain " + domainOp.getDomainName());
				break;
			case DELETE:
				domainModification |= deleteDomain(domainOp.getDomainName(), backend, warningMessages);
				LOG.info("Deleted domain " + domainOp.getDomainName());
				break;
			default:
				throw new RuntimeException("Unknown update operation '" + domainOp.getOperation() + "'");
			}
		}

		if (massDomainUpdate.isFullSync() && containedDomains.size() > 0) {

			Project project = projectMap.values().iterator().next();
			if (project == null) {
				throw new RuntimeException(
						"Something went wrong: there must be a project from pre-processed mass-update-records");
			}

			// delete all project domains that are not contained in the mass update
			for (Backend backend : project.getBackends()) {
				for (Domain domain : backend.getDomainForwardings()) {
					if (!containedDomains.contains(domain.getDomain())) {
						domainModification |= deleteDomain(domain.getDomain(), backend, warningMessages);
						LOG.info("Deleted unreferenced domain " + domain.getDomain());
					}
				}
			}
		}

		// persist config and reload
		// if (domainModification) {
		// haProxyProcessor.manageConfiguration(false, true);
		// }

		// output handling of detached full syncs
		if (massDomainUpdate.isFullSync()) {

			StringBuilder buf = new StringBuilder();
			buf.append("Synchronization finished\n\n");

			if (warningMessages.size() > 0) {
				buf.append("Warnings:\n");
				for (String warning : warningMessages) {
					buf.append(warning).append("\n");
				}
				buf.append("\n");
			}

			if (bos != null) {
				buf.append(bos.toString());
				bos.close();
			}

			emailSender.sendMailToUser(userContext.getApiUser().getEmail(), userContext.getApiUser().getUsername(),
					"AgiPrx Domain-Mass-Update on project " + projectMap.values().iterator().next().getFullname(),
					buf.toString());

			console.unRegisterClient();
		}

		if (username != null) {
			userContext.unregister();
		}

		LOG.info("Finished mass domain update, " + (domainModification ? "changes occurred" : "no changes required"));

		return warningMessages;
	}

	private boolean deleteDomain(String domainName, Backend backend, List<String> warningMessages) throws IOException {

		for (Domain existingDomain : backend.getDomainForwardings()) {
			if (existingDomain.getDomain().equals(domainName)) {
				List<DomainDto> removedDomains = new ArrayList<>();
				domainDao.delete(existingDomain, removedDomains);
				cleanupCertificates(removedDomains, warningMessages);
				return true;
			}
		}

		return false;
	}

	private boolean createOrUpdateDomain(String domainName, Backend backend, boolean enableSSL, String redirectUrl,
			List<String> warningMessages) throws Exception {

		boolean domainModification = false;

		boolean letsencrypt = false;
		boolean certprovided = false;

		if (redirectUrl != null && !validator.isUrl(redirectUrl)) {
			throw new RuntimeException(domainName + ": invalid redirect URL " + redirectUrl);
		}

		if (enableSSL) {

			CertInfo certInfo = haProxyProcessor.getCertInfoForDomainName(domainName);

			// check if domain is covered by any cert
			if (certInfo != null) {
				if (certInfo.isLesslCert()) {
					letsencrypt = true;
				} else {
					certprovided = true;
				}
			} else {
				letsencrypt = true;
			}
		}

		// check if domain exists and needs to be updated
		Domain existingDomain = domainDao.find(domainName, false);
		if (existingDomain != null) {

			// update
			domainModification = true;

			if (!existingDomain.getLetsEncrypt() && letsencrypt) {
				lesslCertProcessor.checkCertAndOptCreate(warningMessages, domainName);
			} else if (existingDomain.getLetsEncrypt() && !letsencrypt) {
				lesslCertProcessor.deleteLesslCert(domainName, warningMessages);
			}

			existingDomain.setBackend(backend);
			existingDomain.setCertProvided(certprovided);
			existingDomain.setLetsEncrypt(letsencrypt);
			existingDomain.setRedirectToUrl(redirectUrl);
			domainDao.update(existingDomain);
		}

		if (!domainModification) {

			// insert
			domainModification = true;

			Domain newDomain = new Domain();
			newDomain.setBackend(backend);
			newDomain.setDomain(domainName);
			newDomain.setCertProvided(certprovided);
			newDomain.setLetsEncrypt(letsencrypt);
			newDomain.setRedirectToUrl(redirectUrl);
			domainDao.create(newDomain);

			if (letsencrypt) {
				lesslCertProcessor.checkCertAndOptCreate(warningMessages, domainName);
			}

		}

		return domainModification;
	}

	public Map<String, DomainDto> findProjectDomains(String projectLabel) {

		Project project = checkAndFindProject(projectLabel);

		Map<String, DomainDto> projectDomains = new HashMap<>();
		for (Backend backend : project.getBackends()) {
			for (Domain domain : backend.getDomainForwardings()) {
				domain.setBackend(backend);
				projectDomains.put(domain.getDomain(), new DomainDto(domain));
			}
		}

		return projectDomains;
	}

	public Map<String, DomainDto> findBackendDomains(String projectLabel, String backendLabel) {

		Project project = checkAndFindProject(projectLabel);
		Backend backend = checkAndFindBackend(project, backendLabel);

		Map<String, DomainDto> domainDtoMap = new HashMap<>();
		for (Domain domain : backend.getDomainForwardings()) {
			domain.setBackend(backend);
			domainDtoMap.put(domain.getDomain(), new DomainDto(domain));
		}

		return domainDtoMap;
	}

	private class DetachedRunner implements Runnable {

		private MassDomainUpdateDto massDomainUpdate;
		private String username;

		public DetachedRunner(MassDomainUpdateDto massDomainUpdate, String username) {
			this.massDomainUpdate = massDomainUpdate;
			this.username = username;
		}

		@Override
		public void run() {
			try {
				NonInteractiveDomainExecutor.this.massDomainUpdateHelper(this.massDomainUpdate, this.username);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void setCommandCompletion() {
		console.setCommandCompletion();
	}
}
