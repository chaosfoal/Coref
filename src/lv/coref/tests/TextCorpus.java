package lv.coref.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lv.coref.data.Sentence;
import lv.coref.data.Text;
import lv.coref.data.Token;
import lv.coref.io.ConllReaderWriter;
import lv.coref.io.ConllReaderWriter.TYPE;
import lv.coref.io.Pipe;
import lv.coref.tests.TextCorpus.Query.QueryResult;
import lv.coref.util.FileUtils;
import lv.coref.util.Pair;
import lv.coref.util.StringUtils;

public class TextCorpus {
	
	public static class Query {
		public static class QueryResult {
			public String text;
			public String lemmaText;
			public String context;
			public String file;
			QueryResult(String text, String lemmaText, String context, String file) {
				this.text = text;
				this.lemmaText = lemmaText;
				this.context = context;
				this.file = file;
			}
			public String toString() {
				return text + " [" + context + "]          (" + file + ")";
			}
		}		
		
		public static enum BitType { WORD, LEMMA, TAG, NER, BEFORE, AFTER, DEP };
		
		private List<List<Pair<BitType, String>>> pattern = new ArrayList<>();
		
		public Query(String queryStrign) {
			String[] wordBits = queryStrign.split("\\s");
			for (String partString : wordBits) {
				List<Pair<BitType, String>> wordPattern = new ArrayList<>();				
				String[] partBits = partString.split("@");
				for (String bit : partBits) {
					if (bit.startsWith("W-")) {
						wordPattern.add(new Pair<BitType,String>(BitType.WORD, bit.substring(2)));
					} else if (bit.startsWith("L-")) {
						wordPattern.add(new Pair<BitType,String>(BitType.LEMMA, bit.substring(2)));
					} else if (bit.startsWith("T-")) {
						wordPattern.add(new Pair<BitType,String>(BitType.TAG, bit.substring(2)));
					} if (bit.startsWith("N-")) {
						wordPattern.add(new Pair<BitType,String>(BitType.NER, bit.substring(2)));
					} else if (bit.startsWith("D-")) {
						wordPattern.add(new Pair<BitType,String>(BitType.DEP, bit.substring(2)));
					} else if (bit.startsWith(">")) {
						wordPattern.add(new Pair<BitType,String>(BitType.BEFORE, ""));
					} else if (bit.startsWith("<")) {
						wordPattern.add(new Pair<BitType,String>(BitType.AFTER, ""));
					}
				}
				pattern.add(wordPattern);
			}
		}
		
		public List<QueryResult> match(Sentence s) {
			List<QueryResult> res = new ArrayList<>();
			
			for (int i = 0; i < s.size(); i++) {
				boolean ok = true;
				StringBuilder text = new StringBuilder();
				StringBuilder lemmaText = new StringBuilder();
				int j = i;
				boolean inside = true;				
				for (List<Pair<BitType, String>> wordPattern : pattern) {
					if (j >= s.size()) { ok = false; break; }
					Token t = s.get(j);
					if (wordPattern.get(0).first.equals(BitType.AFTER)) {
						text = new StringBuilder();
						lemmaText = new StringBuilder();
						continue;
					} else if (wordPattern.get(0).first.equals(BitType.BEFORE)) {
						inside = false;
						continue;
					} else {
						for (Pair<BitType, String> p : wordPattern) {
							if (p.first.equals(BitType.WORD) && !t.getWord().matches(p.second)) { ok = false; break; }
							else if (p.first.equals(BitType.LEMMA) && !t.getLemma().matches(p.second)) { ok = false; break; }
							else if (p.first.equals(BitType.TAG) && !t.getTag().matches(p.second)) { ok = false; break; }
							else if (p.first.equals(BitType.NER) && !(t.getNamedEntity() != null ? t.getNamedEntity().getLabel() : "O").matches(p.second)) { ok = false; break; }
							else if (p.first.equals(BitType.DEP) && !t.getDependency().matches(p.second)){ ok = false; break; }
						}
					}
					if (!ok) break;
					if (inside) {
						text.append(t.getWord()).append(" ");
						lemmaText.append(t.getLemma()).append(" ");
					}
					j++;
				}
				if (ok) {
					res.add(new QueryResult(text.toString(), lemmaText.toString(), s.getTextString(), s.getText().getId()));
				}
			}			
			
			return res;			
		}
		
		public String toString() {
			return pattern.toString();
		}
	}
	
	List<Text> texts = new ArrayList<>();
	
	public void create(String baseDir, int limit, int skip, String endsWith, String outputDir) {
		List<String> files = FileUtils.getFiles(baseDir, limit, skip, endsWith);
		create(files, outputDir);
	}
	
	public void create(List<String> filePaths, String outputDir) {
		Pipe p = new Pipe();
		for (String filePath : filePaths) {
			try {
				File file = new File(filePath);
				String name = StringUtils.getBaseName(file.getName(), ".txt");
				Text text = p.read(filePath);
				String outFile = outputDir + name + ".conll";
				new ConllReaderWriter(TYPE.CONLL).write(outFile, text);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void load(String baseDir, int limit, int skip, String endsWith) {
		List<String> files = FileUtils.getFiles(baseDir, limit, skip, endsWith);
		for (String file : files) {
			try {
				Text t = new ConllReaderWriter().read(file);
				texts.add(t);
				t.setId(file);
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}
	}
	
	public void search(Query q) {
		System.err.println("=====\nSEARCH" + q);
		List<QueryResult> res = new ArrayList<>();
		for (Text t : texts) {
			//System.err.println(t.getId());
			for (Sentence s : t.getSentences()) {
				res.addAll(q.match(s));
			}
		}
		for (QueryResult r : res) {
			System.err.println(r);
		}
	}
	
	public static void main(String[] args) {
		TextCorpus c = new TextCorpus();
//		c.create("data/text_corpus/txt/", -1, -1, ".txt", "D:/work/Coref/data/text_corpus/conll/");

		c.load("data/text_corpus/conll/", -1, -1, ".conll");
		//c.search(new Query("L-\\( W-[^\\d]*@N-^((?!location).)*$ L-\\)"));
		//c.search(new Query("N-profession W-un N-profession"));
//		c.search(new Query("N-person W-un"));
//		c.search(new Query("N-organization W-un"));
		c.search(new Query("D-.*App.*"));
////		
////		
//		c.search(new Query("W-.*@T-(x|y).* > W-\\\" N-organization"));
//		
//		c.search(new Query("W-ES > W-\)"));
//		c.search(new Query("W-projekts > W-korupcijas"));
		
		
//		c.search(new Query("W-RPP"));
//		c.search(new Query("L-Latvija"));
//		c.search(new Query("N-media"));
		
		//c.search(new Query("L-\\( W-.* W-.* L-\\)"));
	}
}
