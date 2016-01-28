package org.sagebionetworks.url;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.sagebionetworks.url.UrlSignerUtils.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.junit.Test;

public class UrlSignerUtilsTest {

	@Test
	public void testMakeS3CanonicalString() throws MalformedURLException {
		HttpMethod method = HttpMethod.POST;
		// All of these urls should generate the same canonical form.
		String[] urls = new String[] {
				"https://host.org/path/child?z=one&a=two&"+HMAC_SIGNATURE+"=abc",
				"http://host.org/path/child?z=one&a=two",
				"http://host.org/path/child?a=two&z=one",
				"http://host.org/path/child?a=two&z=one#refs",
				"http://host.org:8080/path/child?a=two&z=one",
		};
		// All of the urls should generate this canonical form:
		String expectedResult = "POST host.org /path/child?a=two&z=one";
		for (String url : urls) {
			String canonical = UrlSignerUtils.makeS3CanonicalString(method,
					url);
			assertEquals(expectedResult, canonical);
		}
	}
	
	@Test
	public void testMakeS3CanonicalStringNoParams() throws MalformedURLException{
		HttpMethod method = HttpMethod.GET;
		String url = "http://localhost:8080/foo/bar#refs";
		String expectedResult = "GET localhost /foo/bar";
		String canonical = UrlSignerUtils.makeS3CanonicalString(method,
				url);
		assertEquals(expectedResult, canonical);
	}
	
	@Test
	public void testMakeS3CanonicalStringSignatrueNull() throws MalformedURLException{
		HttpMethod method = HttpMethod.GET;
		String url = "http://localhost:8080/foo/bar#refs";
		String expectedResult = "GET localhost /foo/bar";
		String signatureName = null;
		String canonical = UrlSignerUtils.makeS3CanonicalString(method,
				url);
		assertEquals(expectedResult, canonical);
	}
	
	@Test
	public void testMakeS3CanonicalStringOneParams() throws MalformedURLException{
		HttpMethod method = HttpMethod.PUT;
		String url = "http://somehost.net/foo/bar?bar=";
		String expectedResult = "PUT somehost.net /foo/bar?bar=";
		String canonical = UrlSignerUtils.makeS3CanonicalString(method,
				url);
		assertEquals(expectedResult, canonical);
	}
	
	@Test
	public void testGenerateSignature() throws MalformedURLException, NoSuchAlgorithmException{
		String credentials = "a super secret password";
		String signatureParameterName = "signature";
		HttpMethod method = HttpMethod.PUT;
		String url = "http://somehost.net/foo/bar?z=one&a=two&expires=123456";
		String signature = UrlSignerUtils.generateSignature(method, url, credentials);
		assertNotNull(signature);
		String expected = "48139b9703f5f63979b5197db309ec3d09a44ae8";
		assertEquals(expected, signature);
	}

	@Test
	public void testGeneratePreSignedURL() throws MalformedURLException{
		HttpMethod method = HttpMethod.GET;
		String credentials = "a super secret password";
		Date expires = new Date(123L);
		String url = "https://synapse.org/root/folder";
		URL presigned = UrlSignerUtils.generatePreSignedURL(method, url, expires, credentials);
		assertNotNull(presigned);
		String expectedUrl = "https://synapse.org/root/folder?expiration=123&hmacSignature=0736be68b7cfbee8313ed0cc10e612954c8125fc";
		assertEquals(expectedUrl, presigned.toString());
	}
	
	@Test
	public void testGeneratePreSignedURLNullExpires() throws MalformedURLException{
		HttpMethod method = HttpMethod.GET;
		String credentials = "a super secret password";
		Date expires = null;
		String url = "http://synapse.org?foo.bar";
		URL presigned = UrlSignerUtils.generatePreSignedURL(method, url, expires, credentials);
		assertNotNull(presigned);
		String expectedUrl = "http://synapse.org?foo.bar=&hmacSignature=2ac8f03a055b3ce3d14852fe0403e1c8855a22e1";
		assertEquals(expectedUrl, presigned.toString());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGeneratePreSignedURLMethodNull() throws MalformedURLException{
		HttpMethod method = null;
		String credentials = "a super secret password";
		Date expires = new Date(123L);
		String url = "http://synapse.org?foo.bar";
		// call under test.
		UrlSignerUtils.generatePreSignedURL(method, url, expires, credentials);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGeneratePreSignedURLNullURL() throws MalformedURLException{
		HttpMethod method = HttpMethod.GET;
		String credentials = "a super secret password";
		Date expires = new Date(123L);
		String url = null;
		// call under test.
		UrlSignerUtils.generatePreSignedURL(method, url, expires, credentials);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGeneratePreSignedURLCredsNull() throws MalformedURLException{
		HttpMethod method = HttpMethod.GET;
		String credentials = null;
		Date expires = new Date(123L);
		String url = "http://synapse.org?foo.bar";
		// call under test.
		UrlSignerUtils.generatePreSignedURL(method, url, expires, credentials);
	}
	
	@Test
	public void testValidatePresignedURL() throws Exception {
		HttpMethod method = HttpMethod.GET;
		String credentials = "a super secret password";
		Date expires = new Date(System.currentTimeMillis()+(30*1000));
		String url = "http://synapse.org?param1=one&a=two";
		
		URL presignedUrl = UrlSignerUtils.generatePreSignedURL(method, url, expires, credentials);
		// this should be valid
		UrlSignerUtils.validatePresignedURL(method, presignedUrl.toString(), credentials);
	}
	
	@Test
	public void testValidatePresignedURLNoExpires() throws Exception{
		HttpMethod method = HttpMethod.GET;
		String credentials = "a super secret password";
		Date expires = null;
		String url = "http://synapse.org?param1=one&a=two";
		URL presignedUrl = UrlSignerUtils.generatePreSignedURL(method, url, expires, credentials);
		// this should be valid
		UrlSignerUtils.validatePresignedURL(method, presignedUrl.toString(), credentials);
	}
	
	@Test (expected=SignatureExpiredException.class)
	public void testValidatePresignedURLExpired() throws Exception{
		HttpMethod method = HttpMethod.GET;
		String credentials = "a super secret password";
		//expired long ago
		Date expires = new Date(123);
		String url = "http://synapse.org?param1=one&a=two";
		
		URL presignedUrl = UrlSignerUtils.generatePreSignedURL(method, url, expires, credentials);
		// this should be valid
		UrlSignerUtils.validatePresignedURL(method, presignedUrl.toString(), credentials);
	}
	
	@Test (expected=SignatureMismatchException.class)
	public void testValidatePresignedURLMismatch() throws Exception{
		HttpMethod method = HttpMethod.GET;
		String credentials = "a super secret password";
		Date expires = new Date(System.currentTimeMillis()+(30*1000));
		String url = "http://synapse.org?param1=one&a=two";
		
		URL presignedUrl = UrlSignerUtils.generatePreSignedURL(method, url, expires, credentials);
		String preUrl = presignedUrl.toString();
		// change the url
		preUrl = preUrl.replace("one", "onne");
		// this should be valid
		UrlSignerUtils.validatePresignedURL(method, preUrl, credentials);
	}

	
	@Test (expected=IllegalArgumentException.class)
	public void testValidatePresignedURLExpiresFormat() throws Exception {
		HttpMethod method = HttpMethod.GET;
		String credentials = "a super secret password";
		Date expires = null;
		String url = "http://synapse.org?"+EXPIRATION+"=notADate";
		URL presignedUrl = UrlSignerUtils.generatePreSignedURL(method, url, expires, credentials);
		// this should be valid
		UrlSignerUtils.validatePresignedURL(method, presignedUrl.toString(), credentials);
	}
}
