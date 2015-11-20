import gov.nih.nlm.nls.metamap.Ev;
import gov.nih.nlm.nls.metamap.Mapping;
import gov.nih.nlm.nls.metamap.MetaMapApi;
import gov.nih.nlm.nls.metamap.MetaMapApiImpl;
import gov.nih.nlm.nls.metamap.PCM;
import gov.nih.nlm.nls.metamap.Result;
import gov.nih.nlm.nls.metamap.Utterance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class MetaMat2PubAnnot {

	// initialise MetaMap api
	private static MetaMapApi api;

	public MetaMat2PubAnnot(String host, int port) throws MalformedURLException {

		/**
		 * set parameters (set parameters in conf/metamap.properties)
		 */
		api = new MetaMapApiImpl();
		System.out.println(api.DEFAULT_SERVER_HOST);
		api.setHost(host);// "gnode1.mib.man.ac.uk");
		api.setPort(port);

		// get and set parameters
		// List<String> theOptions = FileOps.getFileContentAsList(new
		// File("conf/metamap.parameters").toURI().toURL());
		List<String> theOptions = new ArrayList<String>();
		theOptions.add("-y"); // turn on Word Sense Disambiguation
		theOptions.add("-i");
		theOptions.add("-l");
		// theOptions.add("-R SNOMEDCT,ICD10CM,ICD9CM,ICF,ICF-CY,RXNORM");
		for (String opt : theOptions)
			api.setOptions(opt);

	}

	private static Map<Object, String> getClassification(String term)
			throws Exception {
		// String[] classfication = new String[3];
		Map<Object, String> mp = new HashMap<Object, String>();

		// Certain characters may cause MetaMap to throw an exception;
		// filter terms before passing to mm.
		term = term.replaceAll("'", "");
		term = term.replaceAll("\"", "");

		System.out.println(api.getSession());

		api.setTimeout(5000);

		List<Result> resultList = api.processCitationsFromString(term);
		// Result result = resultList.get(0);
		int i = 0;
		for (Result result : resultList) {
			for (Utterance utterance : result.getUtteranceList()) {
				for (PCM pcm : utterance.getPCMList()) {
					for (Mapping map : pcm.getMappingList()) {
						for (Ev mapEv : map.getEvList()) {

							mp.put(i++, mapEv.getConceptId());
							mp.put(i++, mapEv.getMatchedWords().toString());
							mp.put(i++, mapEv.getPositionalInfo().toString());
							mp.put(i++, mapEv.getSemanticTypes().get(0)); // get
																			// only
																			// first
																			// SemType
																			// //.toString()
																			// for
																			// all
																			// if
																			// applicable
																			// [
																			// SemType1,
																			// SemType2,
																			// etc.]
							mp.put(i++, mapEv.getTerm().getName());
							mp.put(i++, mapEv.getConceptName());
							mp.put(i++, mapEv.getMatchMap().toString());
							mp.put(i++, mapEv.getSources().toString());

							mp.put(i++, mapEv.getPreferredName());
						}
					}
				}
			}
		}
		return mp;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		File theDir = new File("output");

		// if the directory does not exist, create it
		if (!theDir.exists()) {
			System.out.println("creating directory: " + "output");
			boolean result = false;

			try {
				theDir.mkdir();
				result = true;
			} catch (SecurityException se) {
				// handle it
			}
			if (result) {
				System.out.println("DIR created");
			}
		}
		if (args.length < 4) {
			System.out
					.println("Insuficient number of parameters. Needs 4 - MetaMap host, port number, the path to folder with files and sourcedb.");
			return;
		}
		try {
			String host = args[0];
			int port = Integer.parseInt(args[1]); //Default port in MM is 8066
			String pathOfDataSet = args[2];
			String sourcedb = args[3];
			System.out.println("Processing: " + pathOfDataSet);
			File folder = new File(pathOfDataSet);
			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				System.out.println(listOfFiles[i].getName());
				@SuppressWarnings("resource")
				BufferedReader reader = new BufferedReader(new FileReader(
						listOfFiles[i].getAbsoluteFile()));
				String line = null;
				String text = "";
				while ((line = reader.readLine()) != null) {
					text += line + '\n';
				}

				System.out.println("started");
				MetaMat2PubAnnot mp = new MetaMat2PubAnnot(host, port);
				System.out.println("initiated");
				Map<Object, String> aMap = mp.getClassification(text);
				System.out.println("query sent");

				JSONObject js = new JSONObject();
				js.put("text", text);
				js.put("sourcedb", sourcedb);
				js.put("sourceid",
						listOfFiles[i].getName().substring(0,
								listOfFiles[i].getName().length() - 4));
				JSONArray denotations = new JSONArray();

				for (int j = 0; j < aMap.size(); j += 9) {
					String conceptId = aMap.get(j);
					String position = aMap.get(j + 2);
					position = position.substring(2, position.length() - 2);
					String semanticType = aMap.get(j + 3);
					String[] positions;
					if (position.contains("), ")) {
						positions = position.split("\\), \\(");
					} else {
						positions = new String[1];
						positions[0] = position;
					}
					for (int k = 0; k < positions.length; k++) {
						String[] span = positions[k].split(",");
						int start = Integer.parseInt(span[0]);
						int end = start + Integer.parseInt(span[1].trim());

						JSONObject denotation = new JSONObject();
						denotation.put("id", j+"."+k);
						JSONObject jspan = new JSONObject();
						jspan.put("begin", start + "");
						jspan.put("end", end + "");
						denotation.put("span", jspan);
						denotation.put("obj", conceptId);
						denotations.add(denotation);
					}
					js.put("denotations", denotations);
				}

				try (FileWriter file = new FileWriter("output/"
						+ listOfFiles[i].getName().substring(0,
								listOfFiles[i].getName().length() - 4)
						+ ".json")) {
					file.write(js.toJSONString());
					System.out
							.println("Successfully Copied JSON Object to File...");
					// System.out.println("\nJSON Object: " + js);

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
