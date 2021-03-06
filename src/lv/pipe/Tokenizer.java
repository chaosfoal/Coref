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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import lv.label.Annotation;
import lv.label.Labels.LabelIndexAbsolute;
import lv.label.Labels.LabelParagraphs;
import lv.label.Labels.LabelSentences;
import lv.label.Labels.LabelText;
import lv.label.Labels.LabelTokens;
import lv.semti.morphology.analyzer.Analyzer;
import lv.semti.morphology.analyzer.Splitting;
import lv.semti.morphology.analyzer.Word;
import edu.stanford.nlp.sequences.LVMorphologyReaderAndWriter;

public class Tokenizer implements PipeTool {

	private static int SENTENCE_LENGTH_CAP = Splitting.DEFAULT_SENTENCE_LENGTH_CAP;

	private Analyzer analyzer;

	private static Tokenizer instance = null;

	public static Tokenizer getInstance() {
		if (instance == null)
			instance = new Tokenizer();
		if (instance.analyzer == null) {
			LVMorphologyReaderAndWriter.initAnalyzer();
			instance.analyzer = LVMorphologyReaderAndWriter.getAnalyzer();
		}
		return instance;
	}

	@Override
	public void init(Properties prop) {
		// TODO Auto-generated method stub

	}

	public Annotation process(String text) {
		Annotation doc = new Annotation();
		doc.set(LabelText.class, text);
		return process(doc);
	}

	@Override
	public Annotation process(Annotation doc) {
		if (!doc.has(LabelText.class))
			return doc;
		String text = doc.getText();
		String[] paragraphs = text.split("(\\r?\\n)");
		List<Annotation> pLabels = new ArrayList<>(paragraphs.length);
		for (String paragraph : paragraphs) {
			paragraph = paragraph.trim();
			if (paragraph.length() > 0) {
				Annotation pLabel = new Annotation();
				pLabel.setText(paragraph);
				pLabels.add(pLabel);
				processParagraph(pLabel);
			}
		}
		int absSentIdx = 0;
		for (Annotation paragraph : pLabels) {
			if (!paragraph.has(LabelSentences.class)) continue;
			for (Annotation sLabel : paragraph.get(LabelSentences.class)) {
				sLabel.set(LabelIndexAbsolute.class, absSentIdx++);
			}
		}
		doc.set(LabelParagraphs.class, pLabels);
		return doc;
	}

	@Override
	public Annotation processParagraph(Annotation paragraph) {
		if (!paragraph.has(LabelText.class))
			return paragraph;
		String text = paragraph.getText();
		LinkedList<LinkedList<Word>> sentences = Splitting.tokenizeSentences(analyzer, text, SENTENCE_LENGTH_CAP);
		List<Annotation> sLabels = new ArrayList<>();
		for (LinkedList<Word> sentence : sentences) {
			List<Annotation> tLabels = new ArrayList<>();
			// TODO preserve original spacings
			StringBuilder sentStr = new StringBuilder();
			for (Word w : sentence) {
				Annotation tLabel = new Annotation();
				tLabel.set(LabelText.class, w.getToken());
				tLabels.add(tLabel);
				sentStr.append(w.getToken()).append(" ");
			}
			Annotation sLabel = new Annotation();
			sLabel.set(LabelTokens.class, tLabels);
			sLabel.set(LabelText.class, sentStr.toString().trim());
			sLabels.add(sLabel);
		}
		paragraph.set(LabelSentences.class, sLabels);
		return paragraph;
	}

	@Override
	public Annotation processSentence(Annotation sentence) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public List<String> tokenize(String text) {
		List<String> res = new ArrayList<>();
		Annotation a = process(text);
		for (Annotation par : a.get(LabelParagraphs.class)) {
			for (Annotation sent : par.get(LabelSentences.class)) {
				for (Annotation tok : sent.get(LabelTokens.class)) {
					res.add(tok.getText());
				}
			}
		}
		return res;
	}

	public static void main(String[] args) {
		Tokenizer tok = Tokenizer.getInstance();
		Annotation doc = tok
				.process("Uzņēmuma SIA \"Cirvis\" prezidents Jānis Bērziņš. Viņš uzņēmumu vada no 2015. gada.");
		System.out.println(doc.toStringPretty());
		System.out.println(tok.tokenize("Uzņēmuma SIA \"Cirvis\" prezidents Jānis Bērziņš. Viņš uzņēmumu vada no 2015. gada."));
	}

}
