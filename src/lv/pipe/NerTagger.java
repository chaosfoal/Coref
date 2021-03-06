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
package lv.pipe;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import lv.label.Annotation;
import lv.label.Labels.LabelDependency;
import lv.label.Labels.LabelIndex;
import lv.label.Labels.LabelLemma;
import lv.label.Labels.LabelMorphoFeatures;
import lv.label.Labels.LabelParent;
import lv.label.Labels.LabelPosTag;
import lv.label.Labels.LabelPosTagSimple;
import lv.label.Labels.LabelText;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.ListNERSequenceClassifier;
import edu.stanford.nlp.ie.NERClassifierCombiner;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ie.regexp.RegexNERSequenceClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.DistSimAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LVFullTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LVGazAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LVGazFileAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LabelAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.MorphologyFeatureStringAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagGoldAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.ParentAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.StringUtils;

@SuppressWarnings("unchecked")
public class NerTagger implements PipeTool {

	public static final String BOUNDARY = "<s>";
	public static final String OTHER = "O";

	public Properties properties;
	public NERClassifierCombiner nerClassifier;

	private static NerTagger instance = null;

	public static NerTagger getInstance() {
		if (instance == null)
			instance = new NerTagger();
		return instance;
	}

	@Override
	public void init(Properties props) {
		properties = props;
		List<AbstractSequenceClassifier<CoreLabel>> classifiers = new ArrayList<>();
		if (props.containsKey("whiteListCasedLemmas")) classifiers.add(new ListNERSequenceClassifier(props.getProperty("whiteListCasedLemmas"), false, true, true));
		if (props.containsKey("whiteListUncasedWords"))
			classifiers.add(new ListNERSequenceClassifier(props.getProperty("whiteListUncasedWords"), true, false, true));
		if (props.containsKey("whiteListCasedWords"))
			classifiers.add(new ListNERSequenceClassifier(props.getProperty("whiteListCasedWords"), false, false, true));
		if (props.containsKey("loadClassifier")) {
			Properties crfProps = new Properties();
			for (@SuppressWarnings("rawtypes")
			Enumeration propertyNames = props.propertyNames(); propertyNames.hasMoreElements(); ) {
				Object key = propertyNames.nextElement();
				if (key.equals("whiteListCasedLemmas")) continue;
				if (key.equals("whiteListUncasedWords")) continue;
				if (key.equals("whiteListCasedWords")) continue;
				if (key.equals("regexList")) continue;
				crfProps.put(key, props.get(key));
			}
			System.err.println(crfProps);
			try {
				classifiers.add(CRFClassifier.getClassifier(crfProps.getProperty("loadClassifier"), crfProps));
			} catch (ClassCastException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (props.containsKey("regexList"))
			classifiers.add(new RegexNERSequenceClassifier(props.getProperty("regexList"), true, true));
		try {
			nerClassifier = new NERClassifierCombiner(classifiers);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Annotation process(Annotation doc) {
		flattenAndProcess(doc);
		return doc;
	}

	@Override
	public Annotation processParagraph(Annotation paragraph) {
		flattenAndProcess(paragraph);
		return paragraph;
	}

	@Override
	public Annotation processSentence(Annotation sentence) {
		flattenAndProcess(sentence);
		return sentence;
	}

	private void flattenAndProcess(Annotation a) {
		List<Annotation> flat = Annotation.flatten(a, BOUNDARY);
		List<CoreLabel> res = new ArrayList<>(flat.size());
		for (Annotation na : flat) {
			res.add(makeCoreLabel(na));
		}
		nerClassifier.classify(res);
		merge(flat, res);
	}

	private void merge(List<Annotation> flat, List<CoreLabel> result) {
		if (flat.size() != result.size()) {
			System.err.println("Warning: not equal result and annotation set");
		}
		for (int i = 0; i < flat.size(); i++) {
			Annotation a = flat.get(i);
			CoreLabel wi = result.get(i);
			if (a.getText().equals(BOUNDARY))
				continue;

			String answer = wi.get(NamedEntityTagAnnotation.class);
			if (answer == null) {
				answer = wi.get(AnswerAnnotation.class);
				if (answer == null)
					answer = "O";
			}
			if (answer != "O")
				a.setNer(answer);

			// Add extra gazetier features used by NER
			String morphoFeats = a.get(LabelMorphoFeatures.class, "").trim();
			if (wi.get(DistSimAnnotation.class) != null)
				morphoFeats += "|Distsim=" + wi.getString(DistSimAnnotation.class);
			if (wi.get(LVGazAnnotation.class) != null && wi.get(LVGazAnnotation.class).size() > 0)
				morphoFeats += "|Gaz=" + StringUtils.join(wi.get(LVGazAnnotation.class), ",");
			if (wi.get(LVGazFileAnnotation.class) != null && wi.get(LVGazFileAnnotation.class).size() > 0)
				morphoFeats += "|GazFile=" + StringUtils.join(wi.get(LVGazFileAnnotation.class), ",");
			a.set(LabelMorphoFeatures.class, morphoFeats);
		}
	}

	public static CoreLabel makeCoreLabel(Annotation a) {
		CoreLabel wi = new CoreLabel();
		if (!a.has(LabelText.class) || a.getText().equals(BOUNDARY)) {
			wi.setWord(BOUNDARY);
			wi.set(AnswerAnnotation.class, OTHER);
			wi.set(NamedEntityTagGoldAnnotation.class, OTHER);
			wi.setLemma("_");
		} else {
			wi.setWord(a.getText());
		}
		wi.setIndex(a.get(LabelIndex.class, -1));
		wi.setLemma(a.get(LabelLemma.class, "_"));
		wi.set(LVFullTagAnnotation.class, a.get(LabelPosTag.class, "_"));
		wi.setTag(a.get(LabelPosTagSimple.class, "_"));
		wi.set(MorphologyFeatureStringAnnotation.class, a.get(LabelMorphoFeatures.class, "_"));
		wi.set(ParentAnnotation.class, Integer.toString((Integer) a.get(LabelParent.class, -1)));
		wi.set(LabelAnnotation.class, a.get(LabelDependency.class, "_"));
		return wi;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Tokenizer tok = Tokenizer.getInstance();
		Annotation doc = tok
				.process("Uzņēmuma SIA \"Cirvis\" prezidents Jānis Bērziņš. Viņš uzņēmumu vada no 2015. gada.");

		MorphoTagger morpho = MorphoTagger.getInstance();
		Properties morphoProp = new Properties();
		morphoProp.setProperty("morpho.classifierPath", "models/lv-morpho-model.ser.gz");
		morphoProp.setProperty("malt.workingDir", "./models");
		morphoProp.setProperty("malt.extraParams", "-m parse -lfi parser.log");
		morpho.init(morphoProp);
		morpho.process(doc);

		NerTagger ner = NerTagger.getInstance();
		Properties nerProp = new Properties();
		nerProp.load(new FileReader("lv-ner-tagger.prop"));
		ner.init(nerProp);
		ner.process(doc);

		System.err.println(doc.toStringPretty());
	}

}
