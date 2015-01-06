package lv.coref.mf;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lv.coref.data.Mention;
import lv.coref.data.MentionChain;
import lv.coref.data.NamedEntity;
import lv.coref.data.Node;
import lv.coref.data.Paragraph;
import lv.coref.data.Sentence;
import lv.coref.data.Text;
import lv.coref.data.Token;
import lv.coref.io.ConllReaderWriter;
import lv.coref.io.Pipe;
import lv.coref.lv.Constants.Category;
import lv.coref.lv.Constants.PosTag;
import lv.coref.lv.Constants.Type;
import lv.coref.lv.Dictionaries;
import lv.coref.lv.MorphoUtils;
import lv.coref.score.SummaryScorer;
import lv.coref.util.FileUtils;
import lv.coref.util.StringUtils;

public class MentionFinder {

	public static final boolean VERBOSE = false;

	public void findMentions(Text text) {
		for (Paragraph p : text) {
			for (Sentence s : p) {
				findMentionInSentence(s);
			}
		}
	}

	public void findMentionInSentence(Sentence sentence) {
		// addNounMentions(sentence);
		// addNounPhraseMentions(sentence);
		addNamedEntityMentions(sentence);
		addAcronymMentions(sentence);
		addNounPhraseMentions2(sentence);
		addCoordinations(sentence);
		// addCoordinationsFlat(sentence);
		addPronounMentions(sentence);

		MentionCleaner.cleanSentenceMentions(sentence);
		updateMentionHeads(sentence);
		updateMentionBoundaries(sentence);
	}

	private void updateMentionHeads(Sentence sentence) {
		for (Mention m : sentence.getMentions())
			if (m.getHeads().isEmpty())
				m.addHead(m.getLastToken());
	}

	private void updateMentionBoundaries(Sentence sentence) {
		int l = sentence.size();
		for (Mention m : sentence.getMentions()) {

		}
	}

	private void addNamedEntityMentions(Sentence sent) {
		for (NamedEntity n : sent.getNamedEntities()) {
			List<Token> tokens = n.getTokens();
			List<Token> heads = new ArrayList<>();
			heads.add(tokens.get(tokens.size() - 1));
			Mention m = new Mention(sent.getText().getNextMentionID(), tokens,
					heads);
			sent.addMention(m);
			sent.getText().addMentionChain(new MentionChain(m));

			m.setCategory(n.getLabel());

			if (!m.getCategory().equals(Category.unknown)
					&& !m.getCategory().equals(Category.profession)
					&& !m.getCategory().equals(Category.time)
					&& !m.getCategory().equals(Category.sum)) {
				m.setType(Type.NE);
			} else {
				m.setType(Type.NP);
			}
			if (VERBOSE)
				System.err.println("NER named entity " + m);
		}
	}

	private void addNounMentions(Sentence sent) {
		for (Token t : sent) {
			if (t.getTag().startsWith("n")) {
				Mention m = new Mention(sent.getText().getNextMentionID(), t);
				sent.addMention(m);
				sent.getText().addMentionChain(new MentionChain(m));
			}
		}
	}
	private void addAcronymMentions(Sentence sent) {
		for (Token t : sent) {
			if (t.isAcronym()) {
				Mention m = new Mention(sent.getText().getNextMentionID(), t);
				sent.addMention(m);
				sent.getText().addMentionChain(new MentionChain(m));
				m.setType(Type.NE);
				if (VERBOSE)
					System.err.println("ACRONYM mention " + m);
			}
		}
	}

	private void addCoordinations(Sentence sent) {
		Node n = sent.getRootNode();
		addCoordinations(sent, n);
	}

	private void addCoordinationsFlat(Sentence sent) {
		int start = -1;
		int end = -1;
		boolean coord = false;
		for (int i = 0; i < sent.size(); i++) {
			Token t = sent.get(i);
			if (t.getStartMentions().size() > 0) {
				// TODO
			}
		}
	}

	private void addCoordinations(Sentence sent, Node n) {
		for (Node child : n.getChildren()) {
			if (n.getLabel().endsWith("crdParts:crdPart")
					&& n.getHeads().get(0).getPosTag() == PosTag.N) {
				List<Token> tokens = n.getTokens();
				List<Token> heads = new ArrayList<>();
				for (Token t : tokens) {
					if (t.getDependency().endsWith("crdPart")) {
						heads.add(t);
					}
				}
				if (heads.size() > 1) {
					Mention m = new Mention(sent.getText().getNextMentionID(),
							tokens, heads);
					sent.addMention(m);
					sent.getText().addMentionChain(new MentionChain(m));
					m.setType(Type.CONJ);
					// if (VERBOSE)
					System.err.println("MENTION COORDINATION: " + m);
				}
			} else {
				addCoordinations(sent, child);
			}
		}

	}

	private void addPronounMentions(Sentence sent) {
		for (Node n : sent.getNodes(false)) {
			if (n.getHeads().get(0).getPosTag() == PosTag.P) {
				Mention m = new Mention(sent.getText().getNextMentionID(),
						n.getTokens(), n.getHeads());
				sent.addMention(m);
				sent.getText().addMentionChain(new MentionChain(m));
				String text = n.getHeads().get(0).getLemma();
				m.setCategory(Dictionaries.getCategory(text));
				m.setType(Type.PRON);
				m.getLastHeadToken().setPronounType(
						MorphoUtils.getPronounType(m.getLastHeadToken()
								.getTag()));
				m.getLastHeadToken().setPerson(
						MorphoUtils.getPerson(m.getLastHeadToken().getTag()));
			}
		}
	}

	private void addNounPhraseMentions(Sentence sent) {
		for (Node n : sent.getNodes(false)) {
			if (n.getHeads().get(0).getPosTag() == PosTag.N) {
				Mention m = new Mention(sent.getText().getNextMentionID(),
						n.getTokens(), n.getHeads());
				sent.addMention(m);
				sent.getText().addMentionChain(new MentionChain(m));
			}
		}
	}

	private void addNounPhraseMentions2(Sentence sent) {
		for (Node n : sent.getNodes(false)) {
			if (n.getHeads().get(0).getPosTag() == PosTag.N) {
				List<Token> tokens = sent.subList(n.getStart(), n.getHeads()
						.get(n.getHeads().size() - 1).getPosition() + 1);

				// simple filter out incorrect borders due conjunctions,
				// punctuation
				int start = 0, end = tokens.size();
				Set<String> fillerLemmas = new HashSet<String>(Arrays.asList(
						"un", ",", "."));
				while (start < tokens.size()) {
					Token t = tokens.get(start);
					if (fillerLemmas.contains(t.getLemma())) {
						start++;
					} else
						break;
				}
				while (end > 0) {
					Token t = tokens.get(end - 1);
					if (fillerLemmas.contains(t.getLemma())) {
						end--;
					} else
						break;
				}
				for (int i = start; i < end; i++) {
					Token t = tokens.get(i);
					if (t.getPosTag() == PosTag.V)
						start = i + 1;
				}

				if (start > end)
					continue;
				tokens = tokens.subList(start, end);

				Mention m = new Mention(sent.getText().getNextMentionID(),
						tokens, n.getHeads());
				sent.addMention(m);
				sent.getText().addMentionChain(new MentionChain(m));

				m.setType(Type.NP);
				if (m.getFirstToken().isProper())
					m.setType(Type.NE);
			}
		}
	}

	public static void compare() throws Exception {
		ConllReaderWriter rw = new ConllReaderWriter();
		Text text;
		// t = rw.getText("news_63.conll");
		// t = rw.getText("sankcijas.conll");
		text = rw.read("data/corpus/conll/interview_16.conll");
		Text goldText = rw
				.read("data/corpus/corefconll/interview_16.corefconll");
		text.setPairedText(goldText);
		goldText.setPairedText(text);

		MentionFinder mf = new MentionFinder();
		mf.findMentions(text);

		// System.out.println(text);
		for (Sentence s : text.getSentences()) {
			System.out.println(s);
			// System.out.println("\t@" + s.getMentions());
			System.out.println(s.getPairedSentence());

			for (Mention m : s.getPairedSentence().getMentions()) {
				if (m.getMention(true) == null) {
					if (m.getMention(false) == null) {
						System.out.println("\t-- " + m);
					} else {
						System.out.println("\t-+ " + m);
					}
				} else {
					if (m.getMention(false) == null) {
						System.out.println("\t+- " + m);
					} else {
						System.out.println("\t++ " + m);
					}
				}

			}
		}
		SummaryScorer scorer = new SummaryScorer();
		scorer.add(text);
		System.err.println(scorer);
	}

	public static String stringTest(String... strings) {
		String s = stringTests(strings);
		System.out.println(s);
		return s;
	}

	public static String stringTests(String... strings) {
		String stringText = StringUtils.join(strings, "\n");
		StringBuilder sb = new StringBuilder();
		Text t = new Pipe().getText(stringText);
		MentionFinder mf = new MentionFinder();
		mf.findMentions(t);

		for (Sentence s : t.getSentences()) {
			sb.append(s).append("\n");
			for (Mention m : s.getOrderedMentions()) {
				sb.append(" - " + m + "\t\t" + m.toParamString());
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public static void fileTests(BufferedWriter bw, List<String> files)
			throws IOException {

		for (String file : files) {
			bw.write("\n\n\n==== " + file + " ===== \n");

			String fileText = FileUtils.readFile(file, StandardCharsets.UTF_8);
			// System.err.println(stringText);

			String[] parText = fileText.split("\n");
			for (String stringText : parText) {
				stringText = stringText.trim();
				if (stringText.length() == 0)
					continue;
				Text t = new Pipe().getText(stringText);
				MentionFinder mf = new MentionFinder();
				mf.findMentions(t);

				for (Sentence s : t.getSentences()) {
					bw.write(s.toString());
					bw.write("\n");
					for (Mention m : s.getOrderedMentions()) {
						bw.write(" - " + m + "\t\t" + m.toParamString());
						bw.write("\n");
					}
				}
				bw.write("\n\n");
			}
		}
		bw.close();
	}

	public static void main(String[] args) throws IOException {

		// fileTests(new BufferedWriter(new FileWriter("mentionFinder.out")),
		// FileUtils.getFiles("data/mentionTest", -1, -1, ""));

		stringTest("Jānis Kalniņš devās mājup.", "Šodien J.K. devās mājup.",
				"J. Kalniņš devās mājup.",
				"Profesors Jānis Kalniņš devās mājup.",
				"Šodien skolotājs Jānis Kalniņš mācīja ausgtāko matemātiku.");

		stringTest("Latvija, Rīga un Liepāja iestājās par.",
				"Jānis un Pēteris devās mājup.",
				"Uzņēmuma vadītājs un valdes priekšēdētājs Jānis Krūmiņš izteica sašutumu.");

		stringTest("SIA \"Cirvis\". ");
	}

}
