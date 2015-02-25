/*******************************************************************************
 * Copyright 2014,2015 Institute of Mathematics and Computer Science, University of Latvia
 * Author: Artūrs Znotiņš
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package lv.coref.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import lv.coref.data.Text;
import lv.coref.util.FileUtils;

import org.restlet.Client;
import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Protocol;

public class Pipe {
	private final static Logger log = Logger.getLogger(Pipe.class.getName());

	public static String SERVICE = "http://localhost:8182/nertagger";

	private Client client;
	private String service;

	public Pipe() {
		this(SERVICE);
	}

	public Pipe(String service) {
		log.log(Level.FINE, "Init pipe");
		this.service = service;
		client = new Client(Protocol.HTTP);
		try {
			client.start();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Unable to start client", e);
		}
		try {
			// Fix: reread configuration
			LogManager.getLogManager().readConfiguration();
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		log.log(Level.SEVERE, "Init pipe");
	}

	public void close() {
		try {
			client.stop();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Unable to start client", e);
		}
	}

	public Text getText(String text) {
		// try {
		// text = URLEncoder.encode(text, "UTF8");
		// } catch (UnsupportedEncodingException e1) {
		// e1.printStackTrace();
		// }
		Text t = null;
		Request r = new Request();
		r.setResourceRef(service);
		r.setMethod(Method.POST);
		r.setEntity(text, MediaType.TEXT_PLAIN);
		try {
			String result = client.handle(r).getEntityAsText();
			// System.err.println(result);
			StringReader sr = new StringReader(result);
			t = new ConllReaderWriter().read(new BufferedReader(sr));
		} catch (Exception e) {
			log.log(Level.SEVERE, "Unable to get POST response", e);
		}

		return t;
	}

	public Text getTextGet(String text) {
		// try {
		// text = URLEncoder.encode(text, "UTF8");
		// } catch (UnsupportedEncodingException e1) {
		// e1.printStackTrace();
		// }
		Text t = null;
		Request r = new Request();
		r.setResourceRef(service + "/" + text);
		r.setMethod(Method.GET);
		try {
			String result = client.handle(r).getEntityAsText();
			// System.err.println(result);
			StringReader sr = new StringReader(result);
			t = new ConllReaderWriter().read(new BufferedReader(sr));
		} catch (Exception e) {
			log.log(Level.SEVERE, "Unable to get GET response", e);
		}
		return t;
	}

	public Text read(String filename) throws IOException {
		String textString = FileUtils.readFile(filename);
		Text text = getText(textString);
		return text;
	}

	public static void main(String[] args) {
		CorefConfig.logConfig("coref.prop");
		Pipe p = new Pipe();
		System.out.println(p.getText("Jānis Kalniņš devās mājup.\n\nAsta vista."));
		// System.out.println(p.getTextGet("Jānis Kalniņš devās mājup.\n\n\nAsta vista."));
		p.close();
	}
}
