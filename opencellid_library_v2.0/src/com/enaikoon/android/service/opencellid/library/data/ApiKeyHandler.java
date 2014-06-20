/**
 * Copyright 2014 ginstr GmbH
 * 
 * This work is licensed under the Creative Commons Attribution-NonCommercial 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/ 
 *  
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
 */
package com.enaikoon.android.service.opencellid.library.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.enaikoon.android.service.opencellid.library.db.OpenCellIdLibContext;

import android.os.Environment;
import android.util.Log;

/**
 * Use this class to read a random generated API key from
 * a file or to request a new random key from server.
 * 
 * @author Danijel
 * @author Dinko Ivkovic
 *
 */
public class ApiKeyHandler {
	/**
	 * Default URL to request a new random key from.
	 */
	public static final String KEY_GENERATOR_URL_DEFAULT = "/gsmCell/user/generateApiKey";
    
	/**
	 * Default location to store a random generated key.
	 */
    public static final String API_KEY_FILE_DEFAULT = OpenCellIdLibContext.getApplicationDirectoryName() + "apikey.txt";
    
	/**
	 * Default location to store a test random generated key.
	 */
    public static final String API_KEY_FILE_TEST_DEFAULT = OpenCellIdLibContext.getApplicationDirectoryName() + "apikey_test.txt";    
    
    /**
     * Reads a random generated API key from a file stored on external storage.
     * File is located on external storage so it can be accessed from other
     * libraries.
     * 
     * @return null if no key otherwise a key read from file
     */
    private static String getKeyFromFile(boolean testMode) {
    	String keyFilePath = testMode?API_KEY_FILE_TEST_DEFAULT:API_KEY_FILE_DEFAULT;
    	
    	// check if the key file exists
    	File keyFile = new File (keyFilePath);
		if (keyFile.exists()) {
			try {
				FileInputStream fis = new FileInputStream(keyFile);
				int ch = -1;
				StringBuffer sb = new StringBuffer();
				while ((ch = fis.read()) >= 0) {
					sb.append((char) ch);
				}
				fis.close();
				
				// read the key from file
				return sb.toString();
			} catch (Exception e) {
				Log.e(ApiKeyHandler.class.getSimpleName(), "Error reading key from file", e);
			}
		}
		
		return null;
    }
    
    /**
     * Query server to generate a new random API key.
     * Use this if you don't want to register at opencellid.org to
     * get a random key to upload/download data.
     * This function will store the retrieved key to a file.
     * 
     * @return valid API key or null if call failed
     */
    private static String requestNewKey(boolean testMode) {
    	// get a key from server and store it on the SD card
    	String responseFromServer=null;
    	
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(OpenCellIdLibContext.getServerURL(testMode) + KEY_GENERATOR_URL_DEFAULT);
			
			Log.d(ApiKeyHandler.class.getSimpleName(), "Connecting to " + KEY_GENERATOR_URL_DEFAULT + " for a new API key...");
			
			HttpResponse result = httpclient.execute(httpGet);
			
			StatusLine status = result.getStatusLine();
			if (status.getStatusCode() == 200) {
										
				if (result.getEntity() != null) {
					InputStream is = result.getEntity().getContent();

					ByteArrayOutputStream content = new ByteArrayOutputStream();

					// Read response into a buffered stream
					int readBytes = 0;
					byte[] sBuffer = new byte[4096];

					while ((readBytes = is.read(sBuffer)) != -1)
					{
						content.write(sBuffer, 0, readBytes);
					}

					responseFromServer = content.toString();
					
					result.getEntity().consumeContent();
				}
				Log.d(ApiKeyHandler.class.getSimpleName(), "New API key set => " + responseFromServer);
				
				//store new key into defined file
				writeApiKeyToFile(responseFromServer, testMode);
				return responseFromServer;
			} else {
				Log.d(ApiKeyHandler.class.getSimpleName(), "Returned " + status.getStatusCode() + " " + status.getReasonPhrase());
			}
			
			httpclient = null;
			httpGet = null;
			result = null;
			
		} catch (Exception e) {
			Log.e(ApiKeyHandler.class.getSimpleName(), "", e);
		}
		
		return null;
    }
    
    /**
     * stores newly generated key into the file
     * @param key generated by the server
     * @param testMode Defines in which file the key will be stored
     */
    private static void writeApiKeyToFile(String key, boolean testMode) {
    	String keyFilePath = testMode?API_KEY_FILE_TEST_DEFAULT:API_KEY_FILE_DEFAULT;
    	
		File keyFile = new File (keyFilePath);
		
		try {
			FileOutputStream fos = new FileOutputStream(keyFile, false);
			fos.write(key.getBytes());
			fos.flush();
			fos.close();
		} catch (Exception e) {
			Log.e(ApiKeyHandler.class.getSimpleName(), "Error writing key to file", e);
		}
	}
    
    /**
     * get the API key from file if exists, or generate a new one
     * @return API key
     */
    public static String getApiKey()
    {
		String apiKey = ApiKeyHandler.getKeyFromFile(false);
		if (apiKey == null) {
			apiKey = ApiKeyHandler.requestNewKey(false);
		}
		
		return apiKey;
    }
}
