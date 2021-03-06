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
package lv.coref.visual;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import lv.coref.data.Mention;
import lv.coref.data.MentionChain;
import lv.coref.data.Sentence;
import lv.coref.data.Text;
import lv.coref.data.Token;
import lv.coref.io.ConllReaderWriter;
import lv.coref.io.ReaderWriter;
import lv.coref.mf.MentionFinder;
import lv.coref.rules.Ruler;

public class Viewer implements Runnable, ActionListener {

	Text text;
	TextMapping textMapping = new TextMapping();

	private static final int WIDTH = 500;
	private static final int HEIGHT = 800;
	private JFrame frame = new JFrame("LVCoref");
	private JPanel textPanel;
	private JPanel corefPanel;

	public Viewer(Text text) {
		this.text = text;
	}
	
	@Override
	public void run() {
		textPanel = new JPanel();
		
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.PAGE_AXIS));
		//textPanel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setText(text);		
		JScrollPane scrollFrame = new JScrollPane(textPanel);
		frame.add(scrollFrame, BorderLayout.CENTER);
		textPanel.requestFocusInWindow();
		
		corefPanel = new JPanel();
		corefPanel.setLayout(new BoxLayout(corefPanel, BoxLayout.PAGE_AXIS));
		JScrollPane corefScrollFrame = new JScrollPane(corefPanel);
		corefScrollFrame.setPreferredSize(new Dimension(300, -1));
		frame.add(corefScrollFrame, BorderLayout.EAST);
		setCorefPanel(text);
		

//		JButton add = new JButton("Add");
//		add.addActionListener(this);
//		corefPanel.add(add);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(WIDTH, HEIGHT);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		//textPanel.repaint();
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		//setText();
	}
	
	void setCorefPanel(Text text) {
		for (MentionChain mc : text.getMentionChains()) {
			if (mc.size() < 2) continue;
			corefPanel.add(new JLabel("=== " + mc.getID() + " === " + mc.getRepresentative()));
			for (Mention m : mc) {
				StringBuilder sb = new StringBuilder("<html>" + m.toString() + "</html>");
				if (sb.length() > 30) sb.insert(30, "\n\t");
				
				MentionMarker mentionMarker = new MentionMarker(sb.toString(), textMapping, m);
				corefPanel.add(mentionMarker);
			}
		}
	}

	void setText(Text text) {

		List<Sentence> sentences = text.getSentences();
		for (Sentence s : sentences) {
			JPanel sentencePanel = new JPanel();
			sentencePanel.setLayout(new WrapLayout(FlowLayout.LEFT));
			sentencePanel.setSize(new Dimension(WIDTH, 1));

			for (Token t : s) {
				t.getMentions();

				for (Mention m : t.getOrderedStartMentions()) {
					MentionMarker mentionMarker = new MentionMarker(
							"<html><sub>[</sub></html>", textMapping, m);
					textMapping.addMentionMarkerPair(m, mentionMarker);
					sentencePanel.add(mentionMarker);
				}

				TokenMarker tokenMarker = new TokenMarker(t.getWord() + " ",
						textMapping, t);
				textMapping.addTokenMarkerPair(t, tokenMarker);
				sentencePanel.add(tokenMarker);

				for (Mention m : t.getOrderedEndMentions()) {
					String endText = "<html><sub>]</sub></html>";
					if (m.getMentionChain() != null
							&& m.getMentionChain().size() > 1)
						endText = "<html><sub>" + m.getMentionChain().getID()
								+ "]</sub></html>";

					MentionMarker mentionMarker = new MentionMarker(endText,
							textMapping, m);
					// textMapping.addMentionMarkerPair(m, mentionMarker);
					sentencePanel.add(mentionMarker);
				}
			}
			textPanel.add(sentencePanel);
			sentencePanel.setAlignmentX(Container.LEFT_ALIGNMENT);
			
			// break;
		}
	}

	public static void main(final String[] args) throws Exception {
		ReaderWriter rw = new ConllReaderWriter();
		Text text = rw.read("data/test.conll");
		MentionFinder mf = new MentionFinder();
		mf.findMentions(text);
		Ruler r = new Ruler();
		r.resolve(text);
		
//		Text gold = new ConllReaderWriter().getText("data/test.corefconll");
//		text.setPairedText(gold);
//		gold.setPairedText(text);		
		SwingUtilities.invokeLater(new Viewer(text));
	}

}
