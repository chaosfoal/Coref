package integration;

import lv.coref.data.Mention;
import lv.coref.data.Sentence;
import lv.coref.data.Text;
import lv.coref.io.Config;
import lv.coref.io.CorefPipe;
import lv.coref.io.PipeClient;
import lv.coref.lv.Constants.Type;
import lv.label.Annotation;
import lv.label.Labels.LabelMentionType;
import lv.pipe.Coref;
import lv.pipe.Pipe;
import lv.util.StringUtils;

public class MentionTest {
	
	public static boolean USE_PIPE_CLIENT = true;
	public static boolean PRINT_ALL_MENTIONS = true;

	public static void main(String[] args) {
		Config.logInit();
		Config.getInstance().set(Config.PROP_KNB_ENABLE, "false");
		Config.getInstance().set(Config.PROP_COREF_REMOVE_COMMON_UKNOWN_SINGLETONS, "false");
		Config.getInstance().set(Config.PROP_COREF_REMOVE_DESCRIPTOR_MENTIONS, "true");
		Config.getInstance().set(Config.PROP_COREF_REMOVE_SINGLETONS, "false");

		testMention("Šodien biroja vadītājs Pēteris Kalniņš.", "biroja vadītājs", "profession");
		testMention("Šodien Ķīnas sekretāre Inga Liepiņa.", "sekretāre", "profession");
		testMention("Šodien Ķīnas preses sekretāre Inga Liepiņa.", "preses sekretāre", "profession");
		testMention("Šodien preses sekretāre sociālajos jautājumos Inga Liepiņa.", "preses sekretāre", "profession");

		testMention("Šodien veselības ministrs Jānis Bērziņš.", "veselības ministrs", "profession");

		testMention("Šodien Gudrinieku biroja vadītājs Pēteris Kalniņš.", "vadītājs", "profession");
		testMention("Šodien biroja vadītājs Pēteris Kalniņš.", "biroja vadītājs", "profession");

		testMentions("Rīgas reliģiskās draudzes vadītājs Jānis Bērziņš.",
				new MP("Rīgas reliģiskās draudzes", "organization"),
				new MP("vadītājs", "profession"));
		
		testMentions("Šodien reliģiskās draudzes vadītājs Jānis Bērziņš.",
				new MP("reliģiskās draudzes", "organization"),
				new MP("vadītājs", "profession"));
		
		testMentions("Draudzes vadītājs Jānis Bērziņš.",
				new MP("Draudzes vadītājs", "profession"));
		
		testMentions("Rīgas domes Satiksmes departamenta vadītājs Jānis Bērziņš.",
				new MP("Rīgas domes", "organization"),
				new MP("Satiksmes departamenta vadītājs", "profession"),
				new MP("Jānis Bērziņš", "person"));

		testMentions("Vides aizsardzības un reģionālās attīstības ministre Līga Liepiņa.",
				new MP("Vides aizsardzības un reģionālās attīstības ministre", "profession"));

		testMentions("Tieslietu ministrijas Sabiedrisko attiecību nodaļas vadītāja Jana Saulīte",
				new MP("Tieslietu ministrijas", "organization"),
				new MP("Sabiedrisko attiecību nodaļas vadītāja", "profession"));
		
		testMentions("Informēja Valsts kancelejas Preses departamenta vadītājs Aivis Freidenfelds.",
				new MP("Valsts kancelejas", "organization"),
				new MP("Preses departamenta vadītājs", "profession"));
		
		testMentions("Eiropas integrācijas biroja vadītāju Edvardu Kušneru .",
				new MP("Eiropas integrācijas biroja", "organization"),
				new MP("vadītāju", "profession"));
		
		testMentions("Stradiņa slimnīcas Invazīvās un neatliekamās kardioloģijas nodaļas vadītājs Andrejs Ērglis",
				new MP("Stradiņa slimnīcas", "organization"),
				new MP("Invazīvās un neatliekamās kardioloģijas nodaļas vadītājs", "profession"));
		
		testMentions("Šodien biroja vadītājs Edvards Kušners.",
				new MP("biroja vadītājs", "profession"));
		
		
		
		testMentions("Rīgas domes Īpašuma departamenta direktora vietniece.",
				new MP("Rīgas domes", "organization"),
				new MP("Īpašuma departamenta direktora vietniece", "profession"));
		
		testMentions("Labklājības ministrijas Farmācijas departamenta direktora vietniece.",
				new MP("Labklājības ministrijas", "organization"),
				new MP("Farmācijas departamenta direktora vietniece", "profession"));
		
		testMentions("Ārlietu ministrijas Juridiskā departamenta direktors.",
				new MP("Ārlietu ministrijas", "organization"),
				new MP("Juridiskā departamenta direktors", "profession"));
		
		testMentions("Rīgas domes Izglītības, jaunatnes un sporta departamenta direktors Andris Kalniņš.",
				new MP("Rīgas domes", "organization"),
				new MP("Izglītības, jaunatnes un sporta departamenta direktors", "profession"));
		
		testMentions("Rīgas domes Satiksmes departamenta (RDSD) vadītājs.",
				new MP("Rīgas domes Satiksmes departamenta", "organization"),
				new MP("vadītājs", "profession"));
		
		testMentions("Rīgas domes (RD) Satiksmes departamenta vadītājs Edgars Strods.",
				new MP("Rīgas domes", "organization"),
				new MP("Satiksmes departamenta vadītājs", "profession"));
		
		testMentions("SIA „Rīgas ūdens” un Rīgas domes Pilsētas attīstības departamenta vadītājs.",
				new MP("Rīgas domes", "organization"),
				new MP("Pilsētas attīstības departamenta vadītājs", "profession"));
		
		testMentions("Daugavpils pilsētas domes Jaunatnes departamenta vadītājs.",
				new MP("Daugavpils pilsētas domes", "organization"),
				new MP("Jaunatnes departamenta vadītājs", "profession"));
		
		testMentions("Administratīvo lietu departamenta vadītājs.",
				new MP("Administratīvo lietu departamenta vadītājs", "profession"));
		
		testMentions("Eiropas Savienības fondu uzraudzības departamenta vadītājs.",
				new MP("Eiropas Savienības fondu", "organization"),
				new MP("uzraudzības departamenta vadītājs", "profession"));
		
		testMentions("Pilsonības un migrācijas lietu pārvaldes Iedzīvotāju reģistra departamenta vadītājs.",
				new MP("Pilsonības un migrācijas lietu pārvaldes", "organization"),
				new MP("Iedzīvotāju reģistra departamenta vadītājs", "profession"));
		
		testMentions("Satiksmes ministrijas Jūrlietu departamenta vadītājs.",
				new MP("Satiksmes ministrijas", "organization"),
				new MP("Jūrlietu departamenta vadītājs", "profession"));
		
		testMentions("AT departamenta vadītājs.",
				new MP("AT", "organization"),
				new MP("departamenta vadītājs", "profession"));
		
		testMentions("Veselības ekonomikas centra Sabiedrības veselības departamenta direktoru Jāni Bērziņu.",
				new MP("Veselības ekonomikas centra", "organization"),
				new MP("Sabiedrības veselības departamenta direktoru", "profession"));
		
		
		// NEGATIVES
		testMentions("Iekļaus nākotnes komandā.",
				new MP("nākotnes", "location", false));
		
		testMentions("LVF dalībniekiem sacīja.",
				new MP("LVF dalībniekiem", null, false).setType(Type.NE));
		
		testMentions("Jo Īvāne aģentūrai LETA atbildēja.",
				new MP("Īvāne aģentūrai", "organization", false),
				new MP("Īvāne aģentūrai", false).setType(Type.NE));
		
		
		Pipe.close();
		System.exit(0);
	}

	public static boolean testMention(String text, String mentionString, String type) {
		return testMentions(text, new MP(mentionString, type));
	}

	public static boolean testMention(Annotation doc, MP mp) {
		Annotation a = doc.getMention(mp.mentionString, mp.category, mp.par, mp.sent, mp.tok);
		boolean correct = true;
		StringBuilder comment = new StringBuilder();
		if (mp.positive) {
			if (a != null) {
				if (mp.type != null && !mp.type.equals(Type.get(a.get(LabelMentionType.class)))) {
					correct = false;
					comment.append(String.format(" > Incorrect type %s: (expected %s)", a.get(LabelMentionType.class),
							mp.type));
				}
			} else {
				comment.append(String.format(" > Did not found mention"));
				correct = false;
			}
		} else {
			if (a != null) {
				correct = false;
				if (mp.type != null) {
					if (!mp.type.equals(Type.get(a.get(LabelMentionType.class)))) {
						// negative type
						correct = true;
					} else {
						comment.append(String.format(" > Incorrect type: %s (expected NOT %s)",
								a.get(LabelMentionType.class), mp.type));
					}
				}
			} else {
				correct = true;
			}
		}
		System.err.printf("%s %s \"%s\" (%s-%s) \t%s\n", correct ? "+" : "@", mp.positive ? "" : "NOT", mp.mentionString,
				mp.type, mp.category, comment.toString());
		return correct;
	}

	public static boolean testMentions(String text, MP... mentionPlaces) {
		Annotation doc = getAnnotation(text);
		return testMentions(doc, mentionPlaces);
	}

	public static boolean testMentions(Annotation doc, MP... mentionPlaces) {
		System.err.printf("\n==== %s \n", doc.getText().trim());
		System.err.printf("==== %s \n", getFormattedTextString(doc));
		
		if (PRINT_ALL_MENTIONS) {
			Text text = Annotation.makeText(doc);
			CorefPipe.getInstance().process(text);
			for (Sentence s : text.getSentences()) {
				for (Mention m : s.getOrderedMentions()) {
					System.err.printf("\t%s\n", m);
				}
			}
		}

		boolean ok = true;
		for (MP mp : mentionPlaces) {
			ok &= testMention(doc, mp);
		}
		return ok;
	}

	public static String getFormattedTextString(Annotation doc) {
		Text text = Annotation.makeText(doc);
		CorefPipe.getInstance().process(text);
		String str = text.toString();
		if (str != null) {
			str = str.trim().replaceAll("\\r?\\n", " <NEWLINE> ");
		}
		return str;
	}

	public static Annotation getAnnotation(String... strings) {
		String stringText = StringUtils.join(strings, "\n");
		Annotation doc = null;
		if (USE_PIPE_CLIENT) {
			doc = PipeClient.getInstance().getAnnotation(stringText);
			Coref.getInstance().process(doc);
		} else {
			doc = Pipe.getInstance().process(stringText);
		}
		return doc;
	}
}

class MP {
	public String category;
	public String mentionString;
	public int par = -1;
	public int sent = -1;
	public int tok = -1;
	public boolean positive = true;
	public Type type = null;

	MP(String mentionString, String category, int par, int sent, int tok) {
		this.mentionString = mentionString;
		this.category = category;
		this.par = par;
		this.sent = sent;
		this.tok = tok;
	}

	MP(String mentionString, String category) {
		this.mentionString = mentionString;
		this.category = category;
	}
	
	MP(String mentionString, String category, boolean positive) {
		this.mentionString = mentionString;
		this.category = category;
		this.positive = positive;
	}
	
	MP(String mentionString, boolean positive) {
		this.mentionString = mentionString;
		this.category = null;
		this.positive = positive;
	}
	
	public MP setType(Type type) {
		this.type = type;
		return this;
	}
}
