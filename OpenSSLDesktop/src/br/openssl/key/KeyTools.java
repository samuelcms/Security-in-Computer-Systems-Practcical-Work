package br.openssl.key;

import static br.openssl.constants.Constants.KEY_EXTENSION;
import static br.openssl.constants.Constants.MEDIA_PATH;
import static br.openssl.constants.Constants.PRIV_KEY_PREFIX;
import static br.openssl.constants.Constants.PUB_KEY_PATH;
import static br.openssl.constants.Constants.PUB_KEY_PREFIX;
import static br.openssl.constants.Constants.TOKEN_NAME;
import static br.openssl.gui.GraphicUserInterface.showCustomDialog;
import static javax.swing.JOptionPane.ERROR_MESSAGE;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import javax.crypto.BadPaddingException;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.io.InvalidCipherTextIOException;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

public class KeyTools 
{
	public static PrivateKey readEncryptedPrivateKey(String path, String password) throws Exception 
	{
		try (FileInputStream fis = new FileInputStream(path);
				InputStreamReader isr = new InputStreamReader(fis);
				PEMParser pemParser = new PEMParser(isr)) 
		{
			// Lê o objeto PEM do arquivo (a chave).
			Object pemObject = pemParser.readObject();

			// Converte PEM para chave privada usando o provedor Bouncy Castle (BC).
			JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

			try 
			{
				// Verifica o tipo de objeto PEM.
				if (pemObject instanceof PEMEncryptedKeyPair) 
				{
					// Se for um par de chaves PEM criptografado, decifra e retorna a chave privada.
					PEMEncryptedKeyPair encryptedKeyPair = (PEMEncryptedKeyPair) pemObject;
					PEMKeyPair pemKeyPair = encryptedKeyPair.decryptKeyPair(new JcePEMDecryptorProviderBuilder().build(password.toCharArray()));
					return converter.getKeyPair(pemKeyPair).getPrivate();
				} 
				else if (pemObject instanceof PEMKeyPair) 
				{
					// Se for um par de chaves PEM não criptografado, retorna a chave privada.
					PEMKeyPair pemKeyPair = (PEMKeyPair) pemObject;
					return converter.getKeyPair(pemKeyPair).getPrivate();
				} 
				else if (pemObject instanceof PKCS8EncryptedPrivateKeyInfo) 
				{
					// Se for uma chave privada PKCS8 criptografada, decifra e retorne a chave privada.
					PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) pemObject;
					InputDecryptorProvider decProv = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password.toCharArray());
					PrivateKeyInfo privateKeyInfo = encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decProv);
					return converter.getPrivateKey(privateKeyInfo);
				} 
				else 
				{
					// Se o formato da chave privada não for reconhecido, dispara a exceção para notificação.
					throw new IllegalArgumentException("Formato de chave privada não reconhecido");
				}
			} 
			catch (InvalidCipherTextIOException e) 
			{
				// Sinaliza problemas de decifragem (por exemplo, senha incorreta).
				if (e.getCause() instanceof BadPaddingException) 
				{
					BadPaddingException bpe = (BadPaddingException) e.getCause();
					System.out.println("Detalhes do erro: " + bpe.getMessage());
					//System.out.println("Senha incorreta. Por favor, verifique a senha e tente novamente.");
					
					showCustomDialog("Status do Login", "Senha incorreta.", ERROR_MESSAGE);
					
					return null;
				} 
				else 
				{
					// Sinaliza erros desconhecidos durante a leitura da chave privada.
					System.out.println("Erro desconhecido ao ler a chave privada: " + e.getMessage());

					if (e.getCause() != null) 
					{
						System.out.println("Causa: " + e.getCause().getClass().getSimpleName());
						System.out.println("Detalhes: " + e.getCause().getMessage());
					}
					throw e;
				}
			}
		}
	}
		
	public static PublicKey readPublicKey(String path) throws Exception 
	{
		try (FileInputStream fis = new FileInputStream(path);
	         InputStreamReader isr = new InputStreamReader(fis);
	         PEMParser pemParser = new PEMParser(isr)) {

			// Lê o próximo objeto PEM do arquivo.
			Object pemObject = pemParser.readObject();
			
			// System.out.println("Tipo do objeto PEM lido: " + pemObject.getClass().getName());

            // Verifica se o objeto lido é uma chave pública no formato SubjectPublicKeyInfo.
	        if (pemObject instanceof SubjectPublicKeyInfo) {

	        	// Converte o objeto PEM em SubjectPublicKeyInfo.
	        	SubjectPublicKeyInfo publicKeyInfo = (SubjectPublicKeyInfo) pemObject;
	            
	        	// Cria um conversor PEM para chaves usando o provedor Bouncy Castle (BC).
	        	JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
	            
	        	// Converte SubjectPublicKeyInfo em PublicKey usando o conversor e retorna.
	        	return converter.getPublicKey(publicKeyInfo);
	        } 
            // Verifica se o objeto lido é um certificado no formato X509Certificate.
	        else if (pemObject instanceof X509Certificate) 
	        {
	            // Converte o objeto PEM em X509Certificate.
	            X509Certificate certificate = (X509Certificate) pemObject;
	            
	            // Obtém a chave pública do certificado e retorna.
	            return certificate.getPublicKey();

	        }
	        // Verifica se o objeto lido é um certificado no formato X509Certificate.
	        else if (pemObject instanceof X509CertificateHolder) 
	        {
	            // Converte o objeto PEM em X509CertificateHolder.
	            X509CertificateHolder certificateHolder = (X509CertificateHolder) pemObject;

	            // Cria um conversor PEM para chaves usando o provedor Bouncy Castle (BC).
	            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

	            // Converte X509CertificateHolder em X509Certificate usando o conversor e retorna a chave pública.
	            return converter.getPublicKey(certificateHolder.getSubjectPublicKeyInfo());
	        } 
	        
	        else 
	        {
	            // Lança uma exceção indicando que o formato da chave pública não é reconhecido.
	            throw new IllegalArgumentException("Formato de chave pública ou certificado não reconhecido");
	        }
	    } 
		catch (Exception e) 
		{
	        // Captura e relança exceções, se houverem, durante a leitura da chave pública.
	        throw e;
	    }
	}
	
	public static String generateKeyName(String username, boolean privateKey)
	{
		return String.format("%s%s%s", (privateKey) ? PRIV_KEY_PREFIX : PUB_KEY_PREFIX, username, KEY_EXTENSION);
	}

	public static String getPrivKey()
	{
		try 
		{
			String usbPath = String.format("%s/%s", MEDIA_PATH, TOKEN_NAME);
			
			// Executa o comando "ls" par alistra o conteúdo do arquivo e procura alguma chave no Token.
			ProcessBuilder processBuilder = new ProcessBuilder("ls", usbPath);
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;

			// Procurar arquivos com extensão .pem
			while ((line = reader.readLine()) != null) 
				if (line.endsWith(KEY_EXTENSION)) 
					return line;
		} 
		catch (IOException e)
		{
			//e.printStackTrace();
			return null;
		}
    
		return null;
	}
	
	public static boolean userExists(String username) 
	{
		String keyPath = String.format("%s%s", PUB_KEY_PATH, generateKeyName(username, false));
		return Files.exists(Paths.get(keyPath));
	}

} // class KeyTools 
