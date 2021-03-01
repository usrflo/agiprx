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
package de.agitos.agiprx.util;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

// https://jsvnserve.googlecode.com/svn/trunk/src/main/java/com/googlecode/jsvnserve/sshd/PublicKeyReaderUtil.java

public class SshPublicKeyReaderUtil {
	/**
	 * Begin marker for the SECSH public key file format.
	 *
	 * @see #extractSecSHBase64(String)
	 */
	private static final String BEGIN_PUB_KEY = "---- BEGIN SSH2 PUBLIC KEY ----";

	/**
	 * End marker for the SECSH public key file format.
	 *
	 * @see #extractSecSHBase64(String)
	 */
	private static final String END_PUB_KEY = "---- END SSH2 PUBLIC KEY ----";

	/**
	 * Key name of the type of public key for DSA algorithm.
	 *
	 * @see #load(String)
	 */
	private static final String SSH2_DSA_KEY = "ssh-dsa";

	/**
	 * Key name of the type of public key for RSA algorithm.
	 *
	 * @see #load(String)
	 */
	private static final String SSH2_RSA_KEY = "ssh-rsa";

	/**
	 * Default constructor is private so that the public key read utility class
	 * could not be instantiated.
	 */
	private SshPublicKeyReaderUtil() {
	}

	/**
	 * Decodes given public <code>_key</code> text and returns the related public
	 * key instance.
	 *
	 * @param _key
	 *            text key of the encoded public key
	 * @return decoded public key
	 * @throws PublicKeyParseException
	 *             if the public key could not be parsed from <code>_key</code>
	 * @see PublicKeyParseException.ErrorCode#UNKNOWN_PUBLIC_KEY_FILE_FORMAT
	 * @see PublicKeyParseException.ErrorCode#UNKNOWN_PUBLIC_KEY_CERTIFICATE_FORMAT
	 */
	public static PublicKey load(final String _key) throws PublicKeyParseException {
		final int c = _key.charAt(0);

		final String base64;

		if (c == 's') {
			base64 = SshPublicKeyReaderUtil.extractOpenSSHBase64(_key);
		} else if (c == '-') {
			base64 = SshPublicKeyReaderUtil.extractSecSHBase64(_key);
		} else {
			throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.UNKNOWN_PUBLIC_KEY_FILE_FORMAT);
		}

		final SSH2DataBuffer buf = new SSH2DataBuffer(Base64.getDecoder().decode(base64));
		final String type = buf.readString();
		final PublicKey ret;
		if (SshPublicKeyReaderUtil.SSH2_DSA_KEY.equals(type)) {
			ret = decodeDSAPublicKey(buf);
		} else if (SshPublicKeyReaderUtil.SSH2_RSA_KEY.equals(type)) {
			ret = decodePublicKey(buf);
		} else {
			throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.UNKNOWN_PUBLIC_KEY_CERTIFICATE_FORMAT);
		}

		return ret;
	}

	/**
	 * <p>
	 * Extracts from the OpenSSH public key format the base64 encoded SSH public
	 * key.
	 * </p>
	 * <p>
	 * An example of such a definition is:<br/>
	 * <code>ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAIEA1on8gxCGJJWSRT4uOrR130....</code>
	 * </p>
	 *
	 * @param _key
	 *            text of the public key defined in the OpenSSH format
	 * @return base64 encoded public-key data
	 * @throws PublicKeyParseException
	 *             if the OpenSSH public key string is corrupt
	 * @see PublicKeyParseException.ErrorCode#CORRUPT_OPENSSH_PUBLIC_KEY_STRING
	 * @see <a href="http://www.openssh.org">OpenSSH</a>
	 */
	public static String extractOpenSSHBase64(final String _key) throws PublicKeyParseException {
		final String base64;
		try {
			final StringTokenizer st = new StringTokenizer(_key);
			st.nextToken();
			base64 = st.nextToken();
		} catch (final NoSuchElementException e) {
			throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.CORRUPT_OPENSSH_PUBLIC_KEY_STRING);
		}

		return base64;
	}

	/**
	 * <p>
	 * Extracts from the SECSSH public key format the base64 encoded SSH public key.
	 * </p>
	 * <p>
	 * An example of such a definition is:
	 * 
	 * <pre>
	 *  ---- BEGIN SSH2 PUBLIC KEY ----
	 * Comment: This is my public key for use on \
	 * servers which I don't like.
	 * AAAAB3NzaC1kc3MAAACBAPY8ZOHY2yFSJA6XYC9HRwNHxaehvx5wOJ0rzZdzoSOXxbET
	 * W6ToHv8D1UJ/z+zHo9Fiko5XybZnDIaBDHtblQ+Yp7StxyltHnXF1YLfKD1G4T6JYrdH
	 * YI14Om1eg9e4NnCRleaqoZPF3UGfZia6bXrGTQf3gJq2e7Yisk/gF+1VAAAAFQDb8D5c
	 * vwHWTZDPfX0D2s9Rd7NBvQAAAIEAlN92+Bb7D4KLYk3IwRbXblwXdkPggA4pfdtW9vGf
	 * J0/RHd+NjB4eo1D+0dix6tXwYGN7PKS5R/FXPNwxHPapcj9uL1Jn2AWQ2dsknf+i/FAA
	 * vioUPkmdMc0zuWoSOEsSNhVDtX3WdvVcGcBq9cetzrtOKWOocJmJ80qadxTRHtUAAACB
	 * AN7CY+KKv1gHpRzFwdQm7HK9bb1LAo2KwaoXnadFgeptNBQeSXG1vO+JsvphVMBJc9HS
	 * n24VYtYtsMu74qXviYjziVucWKjjKEb11juqnF0GDlB3VVmxHLmxnAz643WK42Z7dLM5
	 * sY29ouezv4Xz2PuMch5VGPP+CDqzCM4loWgV
	 * ---- END SSH2 PUBLIC KEY ----
	 * </pre>
	 * </p>
	 *
	 * @param _key
	 *            text of the public key defined in the SECSH format
	 * @return base64 encoded public-key data
	 * @throws PublicKeyParseException
	 *             if the SECSSH key text file is corrupt
	 * @see PublicKeyParseException.ErrorCode#CORRUPT_SECSSH_PUBLIC_KEY_STRING
	 * @see <a href="http://tools.ietf.org/html/draft-ietf-secsh-publickeyfile">IETF
	 *      Draft for the SECSH format</a>
	 */
	private static String extractSecSHBase64(final String _key) throws PublicKeyParseException {
		final StringBuilder base64Data = new StringBuilder();

		boolean startKey = false;
		boolean startKeyBody = false;
		boolean endKey = false;
		boolean nextLineIsHeader = false;
		for (final String line : _key.split("\n")) {
			final String trimLine = line.trim();
			if (!startKey && trimLine.equals(SshPublicKeyReaderUtil.BEGIN_PUB_KEY)) {
				startKey = true;
			} else if (startKey) {
				if (trimLine.equals(SshPublicKeyReaderUtil.END_PUB_KEY)) {
					endKey = true;
					break;
				} else if (nextLineIsHeader) {
					if (!trimLine.endsWith("\\")) {
						nextLineIsHeader = false;
					}
				} else if (trimLine.indexOf(':') > 0) {
					if (startKeyBody) {
						throw new PublicKeyParseException(
								PublicKeyParseException.ErrorCode.CORRUPT_SECSSH_PUBLIC_KEY_STRING);
					} else if (trimLine.endsWith("\\")) {
						nextLineIsHeader = true;
					}
				} else {
					startKeyBody = true;
					base64Data.append(trimLine);
				}
			}
		}

		if (!endKey) {
			throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.CORRUPT_SECSSH_PUBLIC_KEY_STRING);
		}

		return base64Data.toString();
	}

	/**
	 * <p>
	 * Decodes a DSA public key according to the SSH standard from the data
	 * <code>_buffer</code> based on <b>NIST's FIPS-186</b>. The values of the DSA
	 * public key specification are read in the order
	 * <ul>
	 * <li>prime p</li>
	 * <li>sub-prime q</li>
	 * <li>base g</li>
	 * <li>public key y</li>
	 * </ul>
	 * With the specification the related DSA public key is generated.
	 * </p>
	 *
	 * @param _buffer
	 *            SSH2 data buffer where the type of the key is already read
	 * @return DSA public key instance
	 * @throws PublicKeyParseException
	 *             if the SSH2 public key blob could not be decoded
	 * @see DSAPublicKeySpec
	 * @see <a href=
	 *      "http://en.wikipedia.org/wiki/Digital_Signature_Algorithm">Digital
	 *      Signature Algorithm on Wikipedia</a>
	 * @see <a href="http://tools.ietf.org/html/rfc4253#section-6.6">RFC 4253
	 *      Section 6.6</a>
	 */
	private static PublicKey decodeDSAPublicKey(final SSH2DataBuffer _buffer) throws PublicKeyParseException {
		final BigInteger p = _buffer.readMPint();
		final BigInteger q = _buffer.readMPint();
		final BigInteger g = _buffer.readMPint();
		final BigInteger y = _buffer.readMPint();

		try {
			final KeyFactory dsaKeyFact = KeyFactory.getInstance("DSA");
			final DSAPublicKeySpec dsaPubSpec = new DSAPublicKeySpec(y, p, q, g);

			return dsaKeyFact.generatePublic(dsaPubSpec);

		} catch (final Exception e) {
			throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.SSH2DSA_ERROR_DECODING_PUBLIC_KEY_BLOB,
					e);
		}
	}

	/**
	 * <p>
	 * Decode a RSA public key encoded according to the SSH standard from the data
	 * <code>_buffer</code>. The values of the RSA public key specification are read
	 * in the order
	 * <ul>
	 * <li>public exponent</li>
	 * <li>modulus</li>
	 * </ul>
	 * With the specification the related RSA public key is generated.
	 * </p>
	 *
	 * @param _buffer
	 *            key / certificate data (certificate or public key format
	 *            identifier is already read)
	 * @return RSA public key instance
	 * @throws PublicKeyParseException
	 *             if the SSH2 public key blob could not be decoded
	 * @see RSAPublicKeySpec
	 * @see <a href="http://en.wikipedia.org/wiki/RSA">RSA on Wikipedia</a>
	 * @see <a href="http://tools.ietf.org/html/rfc4253#section-6.6">RFC 4253
	 *      Section 6.6</a>
	 */
	private static PublicKey decodePublicKey(final SSH2DataBuffer _buffer) throws PublicKeyParseException {
		final BigInteger e = _buffer.readMPint();
		final BigInteger n = _buffer.readMPint();

		try {
			final KeyFactory rsaKeyFact = KeyFactory.getInstance("RSA");
			final RSAPublicKeySpec rsaPubSpec = new RSAPublicKeySpec(n, e);

			return rsaKeyFact.generatePublic(rsaPubSpec);

		} catch (final Exception ex) {
			throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.SSH2RSA_ERROR_DECODING_PUBLIC_KEY_BLOB,
					ex);
		}
	}

	/**
	 * The class is used to read from a SSH data buffer the protocol specific
	 * formatting defined in
	 * <a href="http://tools.ietf.org/html/rfc4253#section-6.6">RFC 4253 Section
	 * 6.6</a>.
	 *
	 * @see <a href="http://tools.ietf.org/html/rfc4253#section-6.6">RFC 4253
	 *      Section 6.6</a>
	 */
	private static class SSH2DataBuffer {
		/**
		 * SSH2 data.
		 */
		private final byte[] data;

		/**
		 * Current position in {@link #data}.
		 */
		private int pos;

		/**
		 * Initialize the SSH2 data buffer.
		 *
		 * @param _data
		 *            binaray data blob
		 * @see #data
		 */
		public SSH2DataBuffer(final byte[] _data) {
			this.data = _data;
		}

		/**
		 * Reads a big integer from {@link #data} starting with {@link #pos}. A big
		 * integer is stored as byte array (see {@link #readByteArray()}).
		 *
		 * @return read big integer
		 * @throws PublicKeyParseException
		 *             if the byte array holds not enough bytes
		 * @see #readByteArray()
		 */
		public BigInteger readMPint() throws PublicKeyParseException {
			final byte[] raw = this.readByteArray();
			return (raw.length > 0) ? new BigInteger(raw) : BigInteger.valueOf(0);
		}

		/**
		 * Reads a string from {@link #data} starting with {@link #pos}. A string is
		 * stored as byte array (see {@link #readByteArray()}) in UTF8 format.
		 *
		 * @return read string
		 * @throws PublicKeyParseException
		 *             if the byte array holds not enough bytes
		 * @see #readByteArray()
		 */
		public String readString() throws PublicKeyParseException {
			return new String(this.readByteArray());
		}

		/**
		 * Reads from the {@link #data} starting with {@link #pos} the next four bytes
		 * and prepares an integer.
		 *
		 * @return 32 bit integer value
		 */
		private int readUInt32() {
			final int byte1 = this.data[this.pos++];
			final int byte2 = this.data[this.pos++];
			final int byte3 = this.data[this.pos++];
			final int byte4 = this.data[this.pos++];
			return ((byte1 << 24) + (byte2 << 16) + (byte3 << 8) + (byte4 << 0));
		}

		/**
		 * Reads from the {@link #data} starting with {@link #pos} a byte array. The
		 * byte array is defined as:
		 * <ul>
		 * <li>first the length of the byte array is defined as integer (see
		 * {@link #readUInt32()})</li>
		 * <li>then the byte array itself is defined</li>
		 * </ul>
		 *
		 * @return read byte array from {@link #data}
		 * @throws PublicKeyParseException
		 *             if the byte array holds not enough bytes
		 * @see #readUInt32()
		 * @see PublicKeyParseException.ErrorCode#CORRUPT_BYTE_ARRAY_ON_READ
		 */
		private byte[] readByteArray() throws PublicKeyParseException {
			final int len = this.readUInt32();
			if ((len < 0) || (len > (this.data.length - this.pos))) {
				throw new PublicKeyParseException(PublicKeyParseException.ErrorCode.CORRUPT_BYTE_ARRAY_ON_READ);
			}
			final byte[] str = new byte[len];
			System.arraycopy(this.data, this.pos, str, 0, len);
			this.pos += len;
			return str;
		}
	}

	/**
	 * The Exception is throws if the public key encoded text could not be parsed.
	 * For the related {@link PublicKeyParseException#errorCode} see enumeration
	 * {@link PublicKeyParseException.ErrorCode}.
	 */
	public static final class PublicKeyParseException extends Exception {
		/**
		 * Defines the serialize version unique identifier.
		 */
		private static final long serialVersionUID = 1446034172449421912L;

		/**
		 * Error code of the public key parse exception.
		 */
		private final ErrorCode errorCode;

		/**
		 * Creates a new exception for defined <code>_errorCode</code>.
		 *
		 * @param _errorCode
		 *            error code
		 */
		private PublicKeyParseException(final ErrorCode _errorCode) {
			super(_errorCode.message);
			this.errorCode = _errorCode;
		}

		/**
		 * Creates a new exception for defined <code>_errorCode</code> and
		 * <code>_cause</code>.
		 *
		 * @param _errorCode
		 *            error code
		 * @param _cause
		 *            throwable clause
		 */
		private PublicKeyParseException(final ErrorCode _errorCode, final Throwable _cause) {
			super(_errorCode.message, _cause);
			this.errorCode = _errorCode;
		}

		/**
		 * Returns the error code enumeration of this public key parse exception
		 * instance.
		 *
		 * @return error code of the public key parse exception instance
		 * @see #errorCode
		 */
		public ErrorCode getErrorCode() {
			return this.errorCode;
		}

		/**
		 * Enumeration of the error codes if the public key could not parsed.
		 */
		public enum ErrorCode {
			/**
			 * The format of the given ASCII key is not known and could not be parsed. Only
			 * OpenSSH (starts with 's') and SECSH (starts with '-') are currently
			 * supported.
			 *
			 * @see PublicKeyReaderUtil#load(String)
			 */
			UNKNOWN_PUBLIC_KEY_FILE_FORMAT("Corrupt or unknown public key file format"),

			/**
			 * The binary blob of the key definition used a not supported public key
			 * certificate format. Only DSA and RSA are currently supported.
			 *
			 * @see PublicKeyReaderUtil#SSH2_DSA_KEY
			 * @see PublicKeyReaderUtil#SSH2_RSA_KEY
			 * @see PublicKeyReaderUtil#load(String)
			 */
			UNKNOWN_PUBLIC_KEY_CERTIFICATE_FORMAT("Corrupt or unknown public key certificate format"),

			/**
			 * The public key string is not defined correctly in OpenSSH format.
			 *
			 * @see PublicKeyReaderUtil#extractOpenSSHBase64(String)
			 */
			CORRUPT_OPENSSH_PUBLIC_KEY_STRING("Corrupt OpenSSH public key string"),

			/**
			 * The public key string is not defined correctly in SECSSH format.
			 *
			 * @see PublicKeyReaderUtil#extractSecSHBase64(String)
			 */
			CORRUPT_SECSSH_PUBLIC_KEY_STRING("Corrupt SECSSH public key string"),

			/**
			 * The DSA public key blob could not decoded.
			 *
			 * @see PublicKeyReaderUtil#decodeDSAPublicKey(SSH2DataBuffer)
			 */
			SSH2DSA_ERROR_DECODING_PUBLIC_KEY_BLOB("SSH2DSA: error decoding public key blob"),

			/**
			 * The RSA public key blob could not decoded.
			 *
			 * @see PublicKeyReaderUtil#decodeRSAPublicKey(SSH2DataBuffer)
			 */
			SSH2RSA_ERROR_DECODING_PUBLIC_KEY_BLOB("SSH2RSA: error decoding public key blob"),

			/**
			 * @see PublicKeyReaderUtil.SSH2DataBuffer#readByteArray()
			 */
			CORRUPT_BYTE_ARRAY_ON_READ("Corrupt byte array on read");

			/**
			 * English message of the error code.
			 */
			private final String message;

			/**
			 * Constructor used to initialize the error codes with an error message.
			 *
			 * @param _message
			 *            message text of the error code
			 */
			ErrorCode(final String _message) {
				this.message = _message;
			}
		}
	}
}
