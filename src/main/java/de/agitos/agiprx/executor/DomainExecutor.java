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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.mysql.cj.util.StringUtils;

import de.agitos.agiprx.bean.processor.HAProxyProcessor;
import de.agitos.agiprx.bean.processor.SslCertProcessor;
import de.agitos.agiprx.dao.DomainDao;
import de.agitos.agiprx.db.exception.DuplicateKeyException;
import de.agitos.agiprx.dto.DomainDto;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.Backend;
import de.agitos.agiprx.model.CombinedCertificate;
import de.agitos.agiprx.model.Domain;
import de.agitos.agiprx.output.table.DomainTableFormatter;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.ListUtils;

public class DomainExecutor extends AbstractCertificateRelatedExecutor {

	private static DomainExecutor BEAN;

	private DomainDao domainDao;

	private SslCertProcessor sslCertProcessor;

	private HAProxyProcessor haProxyProcessor;

	public DomainExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		domainDao = DomainDao.getBean();
		sslCertProcessor = SslCertProcessor.getBean();
		haProxyProcessor = HAProxyProcessor.getBean();
	}

	public static DomainExecutor getBean() {
		return BEAN;
	}

	public void run(Backend backend) throws Exception {

		list(backend, null);

		help();

		setCommandCompletion();

		String out;

		while (true) {
			out = console
					.readLine("PROJ " + backend.getProject().getLabel() + " BCKE " + backend.getLabel() + " DOMAIN");

			try {
				handleExitOrAbort(out);
			} catch (AbortionException e) {
				return;
			}

			if (isCommand(CMD_HELP, out)) {
				help();
			} else if (isCommandWithParam(CMD_LS, out)) {
				list(backend, getStringParam(out));
			} else if (isCommand(CMD_LS, out)) {
				list(backend, null);
			} else if (isCommand(CMD_ADD, out)) {
				insert(backend, new Domain());
			} else if (isCommandWithParam(CMD_EDIT, out)) {
				update(backend, getDomainByParam(out));
			} else if (isCommandWithParam(CMD_DEL, out)) {
				delete(backend, getDomainByParam(out));
			} else if (isCommand(CMD_POSTGENLESSLCERTS, out)) {
				postGenerateLESSLCerts(backend);
			} else {
				console.printlnfError("Incorrect command %s", out);
				help();
			}
		}
	}

	private Domain getDomainByParam(String out) {
		Domain domain = null;

		String stringParam = getStringParam(out);
		Long id = getIdOrNull(stringParam);

		if (id != null) {
			domain = domainDao.find(id);
		} else {
			domain = domainDao.find(stringParam);
		}

		return domain;
	}

	private void list(Backend backend, String filter) {
		console.printlnfStress("Backend domains");

		List<Domain> domainList = domainDao.findAllByBackend(backend.getId(),
				filter == null ? null : filter.replaceAll("\\*", "%"));

		DomainTableFormatter tableFormatter = new DomainTableFormatter(console);

		for (Domain model : domainList) {
			model.setBackend(backend); // set back-reference

			tableFormatter.addDomain(model);
		}

		tableFormatter.printTable(console, null);
	}

	private void help() {
		printHelp(CMD_LS + " [*infix*]", "list backend domains, optionally filter by infix, *-wildcard supported");
		printHelp(CMD_ADD, "add new domain");
		printHelp(CMD_EDIT + " <id|domain>", "edit listed domain");
		printHelp(CMD_DEL + " <id|domain>", "delete listed domain");
		printHelp(CMD_POSTGENLESSLCERTS, "generate LESSL certs online as soon as domain IP changed to agiprx");
	}

	protected void setCommandCompletion() {
		console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT, CMD_LS,
				CMD_ADD, CMD_EDIT, CMD_DEL, CMD_POSTGENLESSLCERTS);
	}

	private void editHelper(Backend backend, Domain model) throws Exception {

		String out;

		// domain
		while (true) {
			out = console.readLine("Domain", model.getDomain());

			handleExitOrAbort(out);

			if (model.getDomain() != null && StringUtils.isEmptyOrWhitespaceOnly(out)) {
				break;
			} else if (validator.isDomainName(out)) {
				// check unique domain
				Domain existingDomain = domainDao.find(out);
				if (existingDomain != null && !existingDomain.getId().equals(model.getId())) {
					console.printlnfError(
							"Domain " + out + " is already configured and can only be used once; current use in "
									+ existingDomain.getBackend().getFQLabel());
					continue;
				}
				model.setDomain(out);
				break;
			}
			console.printlnfError("Invalid domain name, try again");
		}

		// letsEncrypt
		while (true) {
			String defaultValue = "n";
			if (Boolean.TRUE.equals(model.getLetsEncrypt())) {
				defaultValue = "y";
			}
			out = console.readLine("Let's Encrypt, (y/n)", defaultValue);

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				out = defaultValue;
			}

			if (isYes(out)) {
				model.setLetsEncrypt(true);
				model.setCertProvided(false);
				break;
			} else if (isNo(out)) {
				model.setLetsEncrypt(false);
				break;
			}

			console.printlnfError("Invalid option, try again");
		}

		if (Boolean.TRUE.equals(model.getLetsEncrypt())) {

			console.printlnf("LESSL certificate generation ...");

			List<String> warningMessages = new ArrayList<String>();
			if (lesslCertProcessor.checkCertAndOptCreate(warningMessages, model.getDomain())) {
				for (String msg : warningMessages) {
					console.printlnf(msg);
				}
				console.printlnf("Processed LESSL certificate for domain %s", model.getDomain());
			} else {
				for (String msg : warningMessages) {
					console.printlnfError(msg);
				}
			}

		} else {

			// certProvided
			while (true) {
				String defaultValue = "n";
				if (model.getCertProvided() != null && model.getCertProvided()) {
					defaultValue = "y";
				}
				out = console.readLine("SSL cert provided, non LESSL, (y/n)", defaultValue);

				handleExitOrAbort(out);

				if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
					if (model.getCertProvided() == null) {
						model.setCertProvided(false);
					}
					break;
				} else if (isYes(out)) {
					model.setCertProvided(true);
					model.setLetsEncrypt(false);
					break;
				} else if (isNo(out)) {
					model.setCertProvided(false);
					break;
				}

				console.printlnfError("Invalid option, try again");
			}

			if (Boolean.TRUE.equals(model.getCertProvided())) {

				boolean enterPrivKeyCert = false;

				while (true) {
					String defaultValue = "n";
					out = console.readLine("Enter Privkey / Cert (y/n)", defaultValue);

					handleExitOrAbort(out);

					if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
						break;
					} else if (isYes(out)) {
						enterPrivKeyCert = true;
						break;
					} else if (isNo(out)) {
						break;
					}

					console.printlnfError("Invalid option, try again");
				}

				if (enterPrivKeyCert) {

					CombinedCertificate cCert = new CombinedCertificate(model.getDomain());

					String defaultValue = "d";
					while (true) {
						out = console.readLine("Enter private key directly (d) or by filename (f)", defaultValue);

						handleExitOrAbort(out);

						if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
							break;
						} else if ("d".equals(out) || "f".equals(out)) {
							break;
						}

						console.printlnfError("Invalid option, try again");
					}

					if ("f".equals(out)) {

						while (true) {
							out = console.readLine("Enter private key absolute filename");

							handleExitOrAbort(out);

							sslCertProcessor.setPrivateKeyByFilename(cCert, out);

							if (cCert.getPrivateKey() != null) {
								break;
							}

							console.printlnfError("Invalid file or format, try again");
						}

					} else {

						while (true) {
							out = console.readLine("Enter private key in PEM format", null, true);

							handleExitOrAbort(out);

							sslCertProcessor.setPrivateKey(cCert, out);

							if (cCert.getPrivateKey() != null) {
								break;
							}

							console.printlnfError("Invalid format, try again");
						}
					}

					while (true) {
						out = console.readLine("Enter certificate directly (d) or by filename (f)", defaultValue);

						handleExitOrAbort(out);

						if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
							break;
						} else if ("d".equals(out) || "f".equals(out)) {
							break;
						}

						console.printlnfError("Invalid option, try again");
					}

					if ("f".equals(out)) {

						while (true) {
							out = console.readLine("Enter certificate absolute filename");

							handleExitOrAbort(out);

							sslCertProcessor.setCertificateByFilename(cCert, out);

							if (cCert.getCertificate() != null) {
								break;
							}

							console.printlnfError("Invalid format, try again");
						}

					} else {

						while (true) {
							out = console.readLine("Enter certificate in PEM format", null, true);

							handleExitOrAbort(out);

							sslCertProcessor.setCertificate(cCert, out);

							if (cCert.getCertificate() != null) {
								break;
							}

							console.printlnfError("Invalid format, try again");
						}
					}

					while (true) {
						out = console.readLine(
								"Enter intermediate certificate/certificates directly (d) or by filename (f) or leave empty (e)",
								defaultValue);

						handleExitOrAbort(out);

						if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
							break;
						} else if ("d".equals(out) || "f".equals(out) || "e".equals(out)) {
							break;
						}

						console.printlnfError("Invalid option, try again");
					}

					if ("f".equals(out)) {

						while (true) {
							out = console.readLine("Enter intermediate certificate(s) absolute filename");

							handleExitOrAbort(out);

							sslCertProcessor.setIntermediateCertificatesByFilename(cCert, out);

							if (cCert.getIntermediateCertificates() != null) {
								break;
							}

							console.printlnfError("Invalid format, try again");
						}

					} else {

						while (true) {
							out = console.readLine("Enter intermediate certificate(s) in PEM format", null, true);

							handleExitOrAbort(out);

							sslCertProcessor.setIntermediateCertificates(cCert, out);

							if (cCert.getIntermediateCertificates() != null) {
								break;
							}

							console.printlnfError("Invalid format, try again");
						}
					}

					sslCertProcessor.installCombinedCertificate(cCert);

				}

			}
		}

		if (Backend.PERMANENT_REDIRECT_LABEL.equals(backend.getLabel())
				|| Backend.TEMPORARY_REDIRECT_LABEL.equals(backend.getLabel())) {

			// redirect target domain
			while (true) {
				out = console.readLine("Redirect to URL", model.getRedirectToUrl());

				handleExitOrAbort(out);

				if (StringUtils.isEmptyOrWhitespaceOnly(out)
						&& !StringUtils.isEmptyOrWhitespaceOnly(model.getRedirectToUrl())) {
					break;
				} else if (validator.isUrl(out)) {
					model.setRedirectToUrl(out);
					break;
				}
				console.printlnfError("Invalid URL, try again");
			}

		} else {
			model.setRedirectToUrl(null);
		}
	}

	public void insert(Backend backend, Domain model) throws Exception {

		model.setBackend(backend);

		try {
			editHelper(backend, model);
		} catch (AbortionException e) {
			return;
		}

		String out = console.readLine("insert? y/n/e", "y");

		if (isEdit(out)) {
			insert(backend, model);
		} else if (isNo(out)) {
			console.printlnf("Canceled insertion");
			return;
		} else {
			try {
				domainDao.create(model);
				backend.getDomainForwardings().add(model);
				console.printlnfStress("Inserted new domain with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private void update(Backend backend, Domain model) throws Exception {

		if (!isValid(backend, model)) {
			list(backend, null);
			return;
		}

		try {
			editHelper(backend, model);
		} catch (AbortionException e) {
			return;
		}

		String out = console.readLine("update? y/n/e", "y");

		if (isEdit(out)) {
			update(backend, model);
		} else if (isNo(out)) {
			console.printlnf("Canceled update");
			return;
		} else {
			try {
				domainDao.update(model);
				ListUtils.replace(backend.getDomainForwardings(), model);
				console.printlnfStress("Updated domain with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private void delete(Backend backend, Domain model) throws Exception {

		if (!isValid(backend, model)) {
			list(backend, null);
			return;
		}

		String out = console.readLine("Please confirm deletion, y/n", "n");

		if (isYes(out)) {

			List<DomainDto> removedDomains = new ArrayList<>();
			if (model.getLetsEncrypt()) {
				out = console.readLine("Delete LESSL cert with config?, y/n", "y");
				if (isNo(out)) {
					removedDomains = null;
				}
			}

			domainDao.delete(model, removedDomains);
			backend.getDomainForwardings().remove(model);
			cleanupCertificates(removedDomains);
			console.printlnfStress("Deleted domain with id %d", model.getId());
		} else {
			console.printlnf("Canceled deletion");
		}
	}

	private void postGenerateLESSLCerts(Backend backend) throws Exception {

		console.printlnf("Online LESSL certificate generation; press any key to cancel");

		List<Domain> lesslDomains = new ArrayList<Domain>();
		for (Domain model : domainDao.findAllByBackend(backend.getId(), null)) {
			if (!model.getLetsEncrypt()) {
				continue;
			}
			model.setBackend(backend); // set back-reference
			lesslDomains.add(model);
		}

		if (lesslDomains.isEmpty()) {
			console.printlnfError("No LESSL domain configured");
			return;
		}

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

		do {

			try {
				Iterator<Domain> iter = lesslDomains.iterator();

				while (iter.hasNext()) {

					Domain model = iter.next();

					List<String> warningMessages = new ArrayList<String>();

					if (lesslCertProcessor.checkCertAndOptCreate(warningMessages, model.getDomain())) {
						for (String msg : warningMessages) {
							console.printlnf("%s - %s: %s", sdf.format(new Date()), model.getDomain(), msg);
						}
						console.printlnf("%s - %s: processed LESSL certificate", sdf.format(new Date()),
								model.getDomain());
						iter.remove();

						haProxyProcessor.generateDomainToCertMap();
						console.printlnfStress("%s - %s: added domain/cert mapping to HAProxy config",
								sdf.format(new Date()), model.getDomain());

					} else {

						for (String msg : warningMessages) {
							console.printlnfError("%s - %s: %s", sdf.format(new Date()), model.getDomain(), msg);
						}
					}

					// test for user interruptions for ms
					if (console.readCharNonBlocking(100) > -1) {
						throw new InterruptedException();
					}
				}

				if (lesslDomains.isEmpty()) {
					console.printlnfStress("Finished");
					return;
				}

				// wait for 10 seconds before next attempt
				// Thread.sleep(10000);

				// wait and test for user interruptions for 10 seconds
				if (console.readCharNonBlocking(10000) > -1) {
					throw new InterruptedException();
				}

			} catch (InterruptedException e) {
				console.printlnfError("Interrupted LESSL certificate generation");
				break;
			}

		} while (true);
	}

	private boolean isValid(Backend backend, Domain model) {

		if (model == null || !domainDao.findAllIdsByBackend(backend.getId()).contains(model.getId())) {
			console.printlnfError("invalid domain, try again");
			return false;
		}

		return true;
	}
}
