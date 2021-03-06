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
package lv.coref.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import lv.coref.lv.Constants.Case;
import lv.coref.lv.Constants.Category;
import lv.coref.lv.Constants.Gender;
import lv.coref.lv.Constants.Number;
import lv.coref.lv.Constants.Person;
import lv.coref.lv.Constants.PosTag;
import lv.coref.lv.Constants.PronType;
import lv.coref.lv.Constants.Type;
import lv.coref.semantic.Entity;

public class Mention implements Comparable<Mention> {
	
	private final static Logger log = Logger.getLogger(Mention.class.getName());

	private MentionChain mentionChain;
	private List<Token> tokens = new ArrayList<>();
	private List<Token> heads = new ArrayList<>();
	private String id;
	private Number number = null;
	private Person person = null;
	private Case mentionCase = null;
	private Gender gender = null;
	private Type type = Type.UNKNOWN;
	private Category category = Category.unknown;
	private Node parent;
	private String text;
	private String normalizedText;

	private List<Mention> descriptorMentions;
	private String comment = "";
	private Boolean isFinal = null; // mention must be kept during processing

	// private Mention mainMention = false;
	// private List<Mention> linkedMentions = new ArrayList<>();
	// private Node span;
	//
	//
	// public Node getSpan() {
	// return span;
	// }
	//
	// public void setSpan(Node span) {
	// this.span = span;
	// }
	// public boolean isLinked() {
	// return isLinked;
	// }
	//
	// public void setLinked(boolean isLinked) {
	// this.isLinked = isLinked;
	// }
	//
	// public List<Mention> getLinkedMentions() {
	// return linkedMentions;
	// }
	//
	// public void setLinkedMentions(List<Mention> linkedMentions) {
	// this.linkedMentions = linkedMentions;
	// }
	//
	public boolean isFinal() {
		if (isFinal != null && isFinal)
			return true;
		return false;
	}

	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}

	public Node getParent() {
		if (parent != null)
			return parent;
		else {
			return getLastHeadToken().getNode().getParent();
		}
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	// public Mention() {
	// // TODO null id for mention chain
	// MentionChain mc = new MentionChain(this);
	// setMentionChain(mc);
	// }

	public Mention(String id, List<Token> tokens, List<Token> heads) {
		for (Token t : tokens) {
			t.addMention(this);
			this.tokens.add(t);
		}
		this.heads = heads;
		this.id = id;
		initializeText(this.tokens);
	}

	public Mention(List<Token> tokens, List<Token> heads) {
		this(null, tokens, heads);
	}

	public Mention(String id, Token token) {
		this.id = id;
		this.tokens.add(token);
		token.addMention(this);
		this.heads.add(token);
		initializeText(this.tokens);
	}

	public void initializeText(List<Token> tokens) {
		StringBuilder text = new StringBuilder();
		StringBuilder normalizedText = new StringBuilder();
		boolean first = true;
		for (Token t : tokens) {
			if (!first) {
				text.append(" ");
				normalizedText.append(" ");
			} else {
				first = false;
			}
			text.append(t.getWord());
			normalizedText.append(t.getLemma());
		}
		this.text = text.toString();
		this.normalizedText = normalizedText.toString();
		// span = new Node("mNode", this.heads);
		// span.setStart(tokens.get(0).getPosition());
		// span.setEnd(tokens.get(tokens.size()-1).getPosition());
	}

	public boolean isProperMention() {
		if (type == Type.NE)
			return true;
		return false;
	}

	public boolean isAcronym() {
		if (tokens.size() > 1)
			return false;
		Token t = getFirstToken();
		return t.isAcronym();
	}

	public boolean isAcronymOf(String acronym) {
		// if (!isProperMention())
		// return false;
		Set<String> exclude = new HashSet<String>(Arrays.asList("un", ",", "\"", "'", "SIA", "AS"));
		String s = "";
		// String[] textWords = text.split(" ");
		for (Token t : getTokens()) {
			if (!exclude.contains(t.getLemma()))
				s += t.getWord().charAt(0);
		}
		return s.toUpperCase().equals(acronym.toUpperCase());
	}

	public boolean isBefore(Mention o) {
		if (o == null)
			return false;
		if (getParagraph().getPosition() > o.getParagraph().getPosition())
			return true;
		if (getParagraph().getPosition() < o.getParagraph().getPosition())
			return false;
		if (getSentence().getPosition() > o.getSentence().getPosition())
			return true;
		if (getSentence().getPosition() < o.getSentence().getPosition())
			return false;
		if (getLastToken().getPosition() > o.getLastToken().getPosition())
			return true;
		if (getLastToken().getPosition() < o.getLastToken().getPosition())
			return false;
		return false;
	}

	public boolean isNestedInside(Mention o) {
		if (getSentence() != o.getSentence())
			return false;
		int ms = getFirstToken().getPosition();
		int me = getLastToken().getPosition();
		int os = o.getFirstToken().getPosition();
		int oe = o.getLastToken().getPosition();
		if (os <= ms && oe >= me)
			return true;
		return false;
	}

	public boolean intersects(Mention o) {
		if (getSentence() != o.getSentence())
			return false;
		// if (isNestedInside(o)) return false;
		int ms = getFirstToken().getPosition();
		int me = getLastToken().getPosition();
		int os = o.getFirstToken().getPosition();
		int oe = o.getLastToken().getPosition();
		if (ms < os && me >= os && me < oe || os < ms && oe >= ms && oe < me)
			return true;
		return false;
	}

	public boolean isMoreRepresentativeThan(Mention o) {
		if (o == null)
			return true;
		// System.err.println(nerString +
		// "("+(category!=null?category:"null")+")"+ " : " + p.nerString +
		// "("+(p.category!=null?p.category:"null")+")");
		if (getType().equals(Type.PRON))
			return false; // PP - lai nav vietniekvārdi kā reprezentatīvākie
		if (o.getType().equals(Type.PRON))
			return true;
		if (!o.getCategory().isUnkown() && !getCategory().isUnkown() && o.getCategory().equals(Category.profession)
				&& getCategory().equals(Category.person) && getType().equals(Type.NE))
			return true;
		if (!o.getCategory().isUnkown() && !getCategory().isUnkown() && o.getCategory().equals(Category.person)
				&& getCategory().equals(Category.profession) && o.getType().equals(Type.NE))
			return false;
		if (!o.getType().equals(Type.NE) && getType().equals(Type.NE))
			return true;
		else if (o.getType().equals(Type.NE) && !getType().equals(Type.NE))
			return false;
		if (getString().length() > o.getString().length())
			return true;
		else if (getString().length() < o.getString().length())
			return false;
		if (isBefore(o))
			return true; // TODO what is better indicator: length or position
		return false;
	}

	public void setTokens(List<Token> tokens) {
		// System.err.println("SET TOKENS " + tokens + " inside: " +
		// getSentence().getTextString());
		for (Token t : this.tokens) {
			t.removeMention(this);
		}
		this.tokens = new ArrayList<>();
		for (Token t : tokens) {
			t.addMention(this);
		}
		this.tokens = tokens;
		initializeText(this.tokens);
		// System.err.println("SETED TOKENS " + tokens + " inside: " +
		// getSentence().getTextString());
	}

	public void setHeads(List<Token> tokens) {
		this.heads = tokens;
	}

	public Category getCategory() {
		return category;
	}
	
	public boolean hasCategory(Category... categories) {
		return this.category.equalsEither(categories);
	}

	public void setCategory(String category) {
		this.category = Category.get(category);
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public List<Token> getTokens() {
		return tokens;
	}

	public Token getFirstToken() {
		return tokens.get(0);
	}

	public Token getLastToken() {
		return tokens.get(tokens.size() - 1);
	}

	public List<Token> getHeads() {
		return heads;
	}

	public Token getLastHeadToken() {
		if (heads.size() > 0) {
			return heads.get(heads.size() - 1);
		} else {
			return getLastToken();
		}
	}

	public void addHead(Token head) {
		heads.add(head);
	}

	public String getID() {
		return id;
	}

	public void setID(String id) {
		this.id = id;
	}

	public Sentence getSentence() {
		return getFirstToken().getSentence();
	}

	public Paragraph getParagraph() {
		return getSentence().getParagraph();
	}

	public Text getText() {
		return getFirstToken().getSentence().getText();
	}

	public MentionChain getMentionChain() {
		return mentionChain;
	}

	public void setMentionChain(MentionChain mentionChain) {
		this.mentionChain = mentionChain;
	}

	// TODO how to tell if is a pronoun
	public boolean isPronoun() {
		if (type == Type.PRON || getLastHeadToken().getPosTag() == PosTag.P)
			return true;
		return false;
	}

	public PronType getPronounType() {
		Token t = getLastHeadToken();
		if (isPronoun())
			return getLastHeadToken().getPronounType();
		return PronType.UNKNOWN;
	}

	public Gender getGender() {
		if (gender != null)
			return gender;
		return getLastHeadToken().getGender();
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public Number getNumber() {
		if (number != null)
			return number;
		return getLastHeadToken().getNumber();
	}

	public void setNumber(Number number) {
		this.number = number;
	}

	public Case getCase() {
		if (mentionCase != null)
			return mentionCase;
		return getLastHeadToken().getTokenCase();
	}

	public void setCase(Case mentionCase) {
		this.mentionCase = mentionCase;
	}

	public Person getPerson() {
		if (person != null)
			return person;
		return getLastHeadToken().getPerson();
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	// public String getLemma() {
	// StringBuffer sb = new StringBuffer();
	// for (Token t : tokens)
	// sb.append(" " + t.getLemma());
	// return sb.toString().trim();
	// }
	//
	// public String getHeadLemma() {
	// StringBuffer sb = new StringBuffer();
	// for (Token t : heads)
	// sb.append(" " + t.getLemma());
	// return sb.toString().trim();
	// }

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	
	public int getSize() {
		return tokens.size();
	}
	
	public int getStart() {
		if (tokens.size() == 0) {
			log.log(Level.WARNING, "getStart: Zero token size mention{0}", this);
			return 0;
		} else {
			return tokens.get(0).getPosition();
		}
	}

	public int getEnd() {
		if (tokens.size() == 0) {
			log.log(Level.WARNING, "getEnd: Zero token size mention {0}", this);
			return 0;
		} else {
			return tokens.get(tokens.size() - 1).getPosition();
		}
	}


	public List<Mention> getDescriptorMentions() {
		return descriptorMentions;
	}

	public void addDescriptorMention(Mention descriptorMention) {
		if (descriptorMentions == null)
			descriptorMentions = new ArrayList<>();
		this.descriptorMentions.add(descriptorMention);
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void addComment(String comment) {
		if (this.comment.length() > 0)
			this.comment += "|";
		this.comment += comment;
	}

	public String getString() {
		// StringBuilder sb = new StringBuilder();
		// boolean first = true;
		// for (Token t : getTokens()) {
		// if (!first)
		// sb.append(" ");
		// else
		// first = false;
		// sb.append(t.getWord());
		// }
		// return sb.toString();
		return text;
	}

	public String getLemmaString() {
		// StringBuilder sb = new StringBuilder();
		// boolean first = true;
		// for (Token t : getTokens()) {
		// if (!first)
		// sb.append(" ");
		// else
		// first = false;
		// sb.append(t.getLemma());
		// }
		// return sb.toString();
		return normalizedText;
	}

	public String getHeadString() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Token t : getHeads()) {
			if (!first)
				sb.append(" ");
			else
				first = false;
			sb.append(t.getWord());
		}
		return sb.toString();
	}

	public String getHeadLemmaString() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Token t : getHeads()) {
			if (!first)
				sb.append(" ");
			else
				first = false;
			sb.append(t.getLemma());
		}
		return sb.toString();
	}

	public Set<String> getProperTokens() {
		Set<String> properTokens = new HashSet<>();
		if (isProperMention()) {
			for (Token t : getTokens()) {
				if (t.isProper())
					properTokens.add(t.getLemma().toLowerCase());
			}
		}
		return properTokens;
	}

	public Set<String> getAttributeTokens() {
		Set<String> attributeTokens = new HashSet<>();
		// if (isProperMention()) {
		for (Token t : getTokens()) {
			if (t.getPosTag().equals(PosTag.ADJ) || t.getPosTag().equals(PosTag.N) || t.getPosTag().equals(PosTag.X))
				attributeTokens.add(t.getLemma().toLowerCase());
		}
		// }
		return attributeTokens;
	}

	public List<Mention> getPotentialAntecedents(int par, int sent, int ment) {
		List<Mention> r = new ArrayList<>();
		int parC = 0, sentC = 0, mentC = 1;
		int pos = getLastToken().getPosition();
		Sentence curSentence = this.getSentence();
		Paragraph curParagraph = curSentence.getParagraph();
		if (curSentence == null || curParagraph == null)
			return r;
		Text text = curParagraph.getText();
		ListIterator<Paragraph> pit = text.listIterator(curParagraph.getPosition() + 1);
		main: while (pit.hasPrevious()) {
			Paragraph p = pit.previous();
			ListIterator<Sentence> sit = p.listIterator(p.size());
			while (sit.hasPrevious()) {
				Sentence s = sit.previous();
				if (p == curParagraph && s.getPosition() > curSentence.getPosition())
					continue;
				List<Mention> mm = s.getMentions();
				ListIterator<Mention> mit = mm.listIterator(mm.size());
				while (mit.hasPrevious()) {
					Mention m = mit.previous();
					if (s == curSentence && pos <= m.getLastToken().getPosition())
						continue;
					r.add(m);
					if (mentC++ >= ment && ment >= 0)
						break main;
				}
				if (sentC++ >= sent && sent >= 0)
					break main;
			}
			if (parC++ >= par && par >= 0)
				break main;
		}
		return r;
	}

	// public int hashCode() {
	// final int prime = 31;
	// int result = 1;
	// result = prime * result + ((heads == null) ? 0 : heads.hashCode());
	// result = prime * result + ((tokens == null) ? 0 : tokens.hashCode());
	// return result;
	// }

	public int compareTo(Mention other) {
		// TODO test performance
		// System.err.println("COMPARE " + this + other);
		Token thisLastToken = getLastToken();
		Token anotherLastToken = other.getLastToken();

		Sentence thisSentence = thisLastToken.getSentence();
		Sentence anotherSentence = anotherLastToken.getSentence();

		Paragraph thisParagraph = thisSentence == null ? null : thisSentence.getParagraph();
		Paragraph anotherParagraph = anotherSentence == null ? null : anotherSentence.getParagraph();

		String thisTextId = thisParagraph == null ? null : thisParagraph.getText().getId();
		String anotherTextId = anotherParagraph == null ? null : anotherParagraph.getText().getId();

		int compare;
		// first, compare by ids of texts
		if (thisTextId != null && anotherTextId != null) {
			compare = thisTextId.compareTo(anotherTextId);
			if (compare != 0)
				return compare;
		}

		// second, compare by paragraph position
		if (thisParagraph != null && anotherParagraph != null) {
			compare = thisParagraph.getPosition().compareTo(anotherParagraph.getPosition());
			if (compare != 0)
				return compare;

			// third, compare by sentence position
			compare = thisSentence.getPosition().compareTo(anotherSentence.getPosition());
			if (compare != 0)
				return compare;
		}

		// fourth, compare by last segments
		compare = thisLastToken.getPosition().compareTo(anotherLastToken.getPosition());
		if (compare != 0)
			return compare;

		// fifth, compare by size
		Integer thisSize = getTokens().size();
		Integer anotherSize = other.getTokens().size();
		compare = thisSize.compareTo(anotherSize);
		if (compare != 0)
			return compare;

		// sixth, compare by last head segments
		Token thisLastHeadSegment = getLastHeadToken();
		Token anotherLastHeadSegment = other.getLastHeadToken();
		if (thisLastHeadSegment != null && anotherLastHeadSegment != null) {
			compare = thisLastHeadSegment.getPosition().compareTo(anotherLastHeadSegment.getPosition());
		}

		// seventh, compare by head segments size
		thisSize = getHeads().size();
		anotherSize = other.getHeads().size();
		compare = thisSize.compareTo(anotherSize);

		return compare;
	}

	@Override
	public boolean equals(Object obj) {
		// if (this == obj)
		// return true;
		// if (obj == null)
		// return false;
		// if (getClass() != obj.getClass())
		// return false;
		// Mention other = (Mention) obj;
		// if (heads == null) {
		// if (other.heads != null)
		// return false;
		// } else if (!heads.equals(other.heads))
		// return false;
		// if (tokens == null) {
		// if (other.tokens != null)
		// return false;
		// } else if (!tokens.equals(other.tokens))
		// return false;
		// return true;
		return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		//sb.append(heads);
		sb.append("[").append(getHeadLemmaString()).append("]");
		sb.append(" ");
		for (Token t : tokens) {
			sb.append(t.toString() + " ");
		}
		sb.append("|").append(getMentionChain() != null ? getMentionChain().getID() : "null");
		if (!getCategory().equals(Category.unknown))
			sb.append("-").append(getCategory());
		sb.append("|").append(getType());
		if (isPronoun())
			sb.append("-" + getPronounType());
		sb.append("|").append(getID());
		sb.append("|").append(getGender());
		sb.append("|").append(getNumber());
		sb.append("|").append(getCase());
		if (comment.length() > 0)
			sb.append("|").append(getComment());
		sb.append("]");
		// sb.append(toParamString());
		return sb.toString();
	}

	// public String getContext(int tokens, int maxWidth) {
	// Sentence s = getFirstToken().getSentence();
	// StringBuilder left = new StringBuilder();
	// StringBuilder right = new StringBuilder();
	// int iTok = 1;
	// for (int i = getFirstToken().getPosition() - 1; i > 0 && (iTok < 0 ||
	// iTok < tokens); i--) {
	// left.insert(0, s.get(i) + " ");
	// if (left.length() > maxWidth && maxWidth >= 0) {
	// left = new StringBuilder(".. " + left.substring(left.length() - maxWidth
	// - 3));
	// break;
	// }
	// iTok++;
	// }
	// iTok = 1;
	// for (int i = getLastToken().getPosition() - 1; i < s.size() && (iTok < 0
	// || iTok < tokens); i++) {
	// right.append(s.get(i)).append(" ");
	// if (right.length() > maxWidth && maxWidth >= 0) {
	// right = new StringBuilder(".. " + right.substring(0, maxWidth - 3));
	// break;
	// }
	// iTok++;
	// }
	// left.append(this).append(right);
	// return left.toString();
	// }

	public String toParamString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("#").append(getID());
		sb.append("*MCne=").append(getMentionChain().isProper());
		sb.append("*MCprop=").append(getMentionChain().getProperTokens());
		sb.append("*").append(getHeadLemmaString());
		sb.append("*").append(getLemmaString());
		sb.append("*").append(getNumber());
		sb.append("*").append(getGender());
		sb.append("*").append(getCase());
		sb.append(String.format("*(%d,%d,%d)", getParagraph().getPosition(), getSentence().getPosition(),
				getLastHeadToken().getPosition()));
		sb.append("*").append(getLastHeadToken().getDependency());
		sb.append("*").append(getLastHeadToken().getParentToken());
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Get Mention from paired Document
	 * 
	 * @return
	 */
	public Mention getMention(boolean exact) {
		Text paired = getSentence().getText().getPairedText();
		if (paired == null)
			return null;
		Sentence pairedSentence = paired.get(getParagraph().getPosition()).get(getSentence().getPosition());
		// System.out.println(getSentence().getPosition() + " " +
		// pairedSentence.getPosition());
		for (Mention m : pairedSentence.getMentions()) {
			boolean equal = true;
			if (exact) {
				if (tokens.size() == m.getTokens().size()) {
					for (int i = 0; i < tokens.size(); i++) {
						if (tokens.get(i).getPosition() != m.getTokens().get(i).getPosition()) {
							equal = false;
							break;
						}
					}
				} else
					equal = false;
			} else {
				if (heads.size() == m.getHeads().size()) {
					for (int i = 0; i < heads.size(); i++) {
						if (heads.get(i).getPosition() != m.getHeads().get(i).getPosition()) {
							equal = false;
							break;
						}
					}
				} else
					equal = false;
			}
			if (equal)
				return m;
		}
		return null;
	}

	public Integer getGlobalId() {
		Entity e = getMentionChain().getEntity();
		if (e == null)
			return null;
		return e.getId();
	}
}
