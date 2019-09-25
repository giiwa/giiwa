/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.framework.web.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.dfile.DFile;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.Disk;
import org.giiwa.framework.web.Controller;

import com.google.common.base.Joiner;
import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.IParse;
import com.vladsch.flexmark.IRender;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.KeepType;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;

public class MarkdownView extends View {

	private String caching = null;

	/**
	 * copy the file to front-end, and {giiwa}/html/ too
	 */
	@Override
	public boolean parse(Object file, Controller m, String viewname) throws IOException {

		InputStream in = null;
		try {

			/**
			 * copy the local html first
			 */
			if (caching == null) {
				caching = Global.getString("web.cache", X.EMPTY);
			}

			String name = View.getName(file);
			String htmlfile = name + ".html";

			if (file instanceof DFile) {
				DFile f = Disk.seek(htmlfile);
				if (!f.exists()) {
					in = View.getInputStream(file);

					BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));

					List<String> list = reader.lines().collect(Collectors.toList());
					String content = Joiner.on("\n").join(list);

					String html = parse(content);

					PrintStream out = new PrintStream(f.getOutputStream());
					out.println(html);
					X.close(in, out);
				}

				in = f.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
				List<String> list = reader.lines().collect(Collectors.toList());
				String content = Joiner.on("\n").join(list);
				X.close(reader, in);
				in = null;

				m.set("name", ((DFile) file).getName());
				m.set("html", content);
				m.show("/admin/md.html");

			} else {

				File f = new File(htmlfile);
				if (!f.exists()) {
					// process as html
					in = View.getInputStream(file);

					BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));

					List<String> list = reader.lines().collect(Collectors.toList());
					String content = Joiner.on("\n").join(list);

					String html = parse(content);
					PrintStream out = new PrintStream(new FileOutputStream(htmlfile));
					out.println(html);
					X.close(in, out);
				}

				in = new FileInputStream(htmlfile);

				BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
				List<String> list = reader.lines().collect(Collectors.toList());
				String content = Joiner.on("\n").join(list);
				X.close(reader, in);
				in = null;

				m.set("name", ((File) file).getName());
				m.set("html", content);
				m.show("/admin/md.html");

			}

			return true;
		} finally {
			X.close(in);
		}
	}

	public static String parse(String content) {

		DataHolder dataHolder = options();
		IParse parser = Parser.builder(dataHolder).build();
		IRender render = HtmlRenderer.builder(dataHolder).build();

		Node document = parser.parse(content);
		return render.render(document);

	}

	private static MutableDataHolder _dataholder;

	public static void setHolder(MutableDataHolder holder) {
		_dataholder = holder;
	}

	private static MutableDataHolder options() {
		return _dataholder;
	}

	static {
		Options options = new Options();

		MutableDataSet dataSet = new MutableDataSet();
		ArrayList<Extension> extensions = new ArrayList<>();

		dataSet.set(Parser.PARSE_INNER_HTML_COMMENTS, true);
		dataSet.set(Parser.INDENTED_CODE_NO_TRAILING_BLANK_LINES, true);
		dataSet.set(HtmlRenderer.SUPPRESS_HTML_BLOCKS, false);
		dataSet.set(HtmlRenderer.SUPPRESS_INLINE_HTML, false);

		// add default extensions in pegdown
		// extensions.add(EscapedCharacterExtension.create());

		// Setup Block Quote Options
		dataSet.set(Parser.BLOCK_QUOTE_TO_BLANK_LINE, true);

		// Setup List Options for GitHub profile
		dataSet.set(Parser.LISTS_AUTO_LOOSE, false);
		// dataSet.set(Parser.LISTS_BULLET_MATCH, false);
		// dataSet.set(Parser.LISTS_ITEM_TYPE_MATCH, false);
		// dataSet.set(Parser.LISTS_ITEM_MISMATCH_TO_SUBITEM, false);
		dataSet.set(Parser.LISTS_END_ON_DOUBLE_BLANK, false);
		// dataSet.set(Parser.LISTS_FIXED_INDENT, 4);
		dataSet.set(Parser.LISTS_BULLET_ITEM_INTERRUPTS_PARAGRAPH, false);
		dataSet.set(Parser.LISTS_BULLET_ITEM_INTERRUPTS_ITEM_PARAGRAPH, true);
		dataSet.set(Parser.LISTS_ORDERED_ITEM_DOT_ONLY, true);
		dataSet.set(Parser.LISTS_ORDERED_ITEM_INTERRUPTS_PARAGRAPH, false);
		dataSet.set(Parser.LISTS_ORDERED_ITEM_INTERRUPTS_ITEM_PARAGRAPH, true);
		dataSet.set(Parser.LISTS_ORDERED_NON_ONE_ITEM_INTERRUPTS_PARAGRAPH, false);
		// dataSet.set(Parser.LISTS_ORDERED_NON_ONE_ITEM_INTERRUPTS_PARENT_ITEM_PARAGRAPH,
		// true);
		dataSet.set(Parser.LISTS_ORDERED_LIST_MANUAL_START, false);

		// if (options.abbreviations) {
		// extensions.add(AbbreviationExtension.create());
		// dataSet.set(AbbreviationExtension.ABBREVIATIONS_KEEP, KeepType.LAST);
		// }

		// if (options.anchorLinks) {
		// extensions.add(AnchorLinkExtension.create());
		// dataSet.set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, true);
		// }

		// if (options.autoLinks) {
		// extensions.add(AutolinkExtension.create());
		// }

		// if (options.definitions) {
		// // not implemented yet, but have placeholder
		// extensions.add(DefinitionExtension.create());
		// }

		if (options.fencedCode) {
			// disable fenced code blocks
			dataSet.set(Parser.MATCH_CLOSING_FENCE_CHARACTERS, false);
		} else {
			dataSet.set(Parser.FENCED_CODE_BLOCK_PARSER, false);
		}

		if (options.hardWraps) {
			dataSet.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
			dataSet.set(HtmlRenderer.HARD_BREAK, "<br />\n<br />\n");
		}

		if (!options.atxHeadingSpace) {
			dataSet.set(Parser.HEADING_NO_ATX_SPACE, true);
		}
		dataSet.set(Parser.HEADING_NO_LEAD_SPACE, true);

		// if (options.typographicQuotes || options.typographicSmarts) {
		// // not implemented yet, have placeholder
		// extensions.add(TypographicExtension.create());
		// dataSet.set(TypographicExtension.TYPOGRAPHIC_SMARTS,
		// options.typographicSmarts);
		// dataSet.set(TypographicExtension.TYPOGRAPHIC_QUOTES,
		// options.typographicQuotes);
		// }

		dataSet.set(Parser.THEMATIC_BREAK_RELAXED_START, options.relaxedThematicBreak);

		// if (options.strikeThrough) {
		// extensions.add(StrikethroughExtension.create());
		// }

		// if (options.tables) {
		// extensions.add(TablesExtension.create());
		// dataSet.set(TablesExtension.TRIM_CELL_WHITESPACE, false);
		// dataSet.set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, false);
		// }

		// if (options.taskListItems) {
		// extensions.add(TaskListExtension.create());
		// }

		// if (options.wikiLinks) {
		// extensions.add(WikiLinkExtension.create());
		// dataSet.set(WikiLinkExtension.LINK_FIRST_SYNTAX, !options.wikiLinkGfmSyntax);
		// }

		// if (options.footnotes) {
		// extensions.add(FootnoteExtension.create());
		// dataSet.set(FootnoteExtension.FOOTNOTES_KEEP, KeepType.LAST);
		// }

		// References compatibility
		dataSet.set(Parser.REFERENCES_KEEP, KeepType.LAST);

		// if (options.tableOfContents) {
		// extensions.add(SimTocExtension.create());
		// dataSet.set(SimTocExtension.BLANK_LINE_SPACER, true);
		//
		// extensions.add(TocExtension.create());
		// dataSet.set(TocExtension.LEVELS, TocOptions.getLevels(2, 3));
		// }

		// if (options.jekyllFrontMatter) {
		// extensions.add(JekyllFrontMatterExtension.create());
		// }

		// if (options.emojiShortcuts) {
		// // requires copying the emoji images to some directory and setting it here
		// extensions.add(EmojiExtension.create());
		// if (options.emojiImageDirectory.isEmpty()) {
		// // dataSet.set(EmojiExtension.USE_IMAGE_URLS, true);
		// } else {
		// dataSet.set(EmojiExtension.ROOT_IMAGE_PATH, options.emojiImageDirectory);
		// }
		// }

		// set rendering options for Swing
		dataSet.set(HtmlRenderer.INDENT_SIZE, 2);
		// dataSet.set(Parser.LISTS_LOOSE_ON_PREV_LOOSE_ITEM, false);

		if (options.fencedCode) {
			dataSet.set(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "");
		}

		// if (options.tables && options.renderTablesGFM) {
		// dataSet.set(TablesExtension.COLUMN_SPANS,
		// false).set(TablesExtension.MIN_HEADER_ROWS, 1)
		// .set(TablesExtension.MAX_HEADER_ROWS,
		// 1).set(TablesExtension.APPEND_MISSING_COLUMNS, true)
		// .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
		// .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);
		// }

		dataSet.set(HtmlRenderer.RENDER_HEADER_ID, false);
		dataSet.set(HtmlRenderer.GENERATE_HEADER_ID, true);

		// set flexmark example spec rendering
		// dataSet.set(SpecExampleExtension.SPEC_EXAMPLE_RENDER_AS,
		// RenderAs.FENCED_CODE);
		// dataSet.set(SpecExampleExtension.SPEC_EXAMPLE_RENDER_RAW_HTML, false);

		dataSet.set(Parser.EXTENSIONS, extensions);

		_dataholder = dataSet;
	}

	public static class Options {
		public boolean abbreviations = false;
		public boolean autoLinks = true;
		public boolean anchorLinks = true;
		public boolean definitions = false;
		public boolean fencedCode = true;
		public boolean hardWraps = false;
		public boolean atxHeadingSpace = true;
		public boolean typographicQuotes = false;
		public boolean typographicSmarts = false;
		public boolean relaxedThematicBreak = true;
		public boolean strikeThrough = true;
		public boolean tables = true;
		public boolean renderTablesGFM = true;
		public boolean taskListItems = true;
		public boolean wikiLinks = false;
		public boolean wikiLinkGfmSyntax = true;
		public boolean footnotes = false;
		public boolean tableOfContents = true;
		public boolean jekyllFrontMatter = false;
		public boolean emojiShortcuts = false;
		public String emojiImageDirectory = "";
	}

	@Override
	public String parse(Object file, JSON params) {
		// TODO Auto-generated method stub
		return null;
	}

}
