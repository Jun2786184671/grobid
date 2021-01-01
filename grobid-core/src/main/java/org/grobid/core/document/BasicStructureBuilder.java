package org.grobid.core.document;

import com.google.common.collect.Iterables;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

import org.apache.commons.lang3.tuple.Pair;

import org.grobid.core.data.BibDataSet;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.layout.Block;
import org.grobid.core.layout.Cluster;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.TextUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for building basic structures in a document item.
 *
 * @author Patrice Lopez
 */
public class BasicStructureBuilder {
	private static final Logger LOGGER = LoggerFactory.getLogger(BasicStructureBuilder.class);

	// note: these regular expressions will disappear as a new CRF model is now covering 
	// the overall document segmentation
    static public Pattern introduction =
            Pattern.compile("^\\b*(Introduction?|Einleitung|INTRODUCTION|Acknowledge?ments?|Acknowledge?ment?|Background?|Content?|Contents?|Motivations?|1\\.\\sPROBLEMS?|1\\.(\\n)?\\sIntroduction?|1\\.\\sINTRODUCTION|I\\.(\\s)+Introduction|1\\.\\sProblems?|I\\.\\sEinleitung?|1\\.\\sEinleitung?|1\\sEinleitung?|1\\sIntroduction?)",
                    Pattern.CASE_INSENSITIVE);
    static public Pattern introductionStrict =
            Pattern.compile("^\\b*(1\\.\\sPROBLEMS?|1\\.(\\n)?\\sIntroduction?|1\\.(\\n)?\\sContent?|1\\.\\sINTRODUCTION|I\\.(\\s)+Introduction|1\\.\\sProblems?|I\\.\\sEinleitung?|1\\.\\sEinleitung?|1\\sEinleitung?|1\\sIntroduction?)",
                    Pattern.CASE_INSENSITIVE);
    static public Pattern abstract_ = Pattern.compile("^\\b*\\.?(abstract?|résumé?|summary?|zusammenfassung?)",
            Pattern.CASE_INSENSITIVE);
    /*static public Pattern keywords = Pattern.compile("^\\b*\\.?(keyword?|key\\s*word?|mots\\s*clefs?)",
            Pattern.CASE_INSENSITIVE);*/
    /*static public Pattern references =
            Pattern.compile("^\\b*(References?|REFERENCES?|Bibliography|BIBLIOGRAPHY|" +
                    "References?\\s+and\\s+Notes?|References?\\s+Cited|REFERENCE?\\s+CITED|REFERENCES?\\s+AND\\s+NOTES?|Références|Literatur|" +
                    "LITERATURA|Literatur|Referências|BIBLIOGRAFIA|Literaturverzeichnis|Referencias|LITERATURE CITED|References and Notes)", Pattern.CASE_INSENSITIVE);*/
    /*static public Pattern header = Pattern.compile("^((\\d\\d?)|([A-Z](I|V|X)*))(\\.(\\d)*)*\\s(\\D+)");*/
    //    static public Pattern header2 = Pattern.compile("^\\d\\s\\D+");
    /*static public Pattern figure = Pattern.compile("(figure\\s|fig\\.|sch?ma)", Pattern.CASE_INSENSITIVE);
    static public Pattern table = Pattern.compile("^(T|t)able\\s|tab|tableau", Pattern.CASE_INSENSITIVE);
    static public Pattern equation = Pattern.compile("^(E|e)quation\\s");
    private static Pattern acknowledgement = Pattern.compile("(acknowledge?ments?|acknowledge?ment?)",
            Pattern.CASE_INSENSITIVE);*/
    static public Pattern headerNumbering1 = Pattern.compile("^(\\d+)\\.?\\s");
    static public Pattern headerNumbering2 = Pattern.compile("^((\\d+)\\.)+(\\d+)\\s");
    static public Pattern headerNumbering3 = Pattern.compile("^((\\d+)\\.)+\\s");
    static public Pattern headerNumbering4 = Pattern.compile("^([A-Z](I|V|X)*(\\.(\\d)*)*\\s)");
//    static public Pattern enumeratedList = Pattern.compile("^|\\s(\\d+)\\.?\\s");

    private static Pattern startNum = Pattern.compile("^(\\d)+\\s");
    private static Pattern endNum = Pattern.compile("\\s(\\d)+$");

    /**
     * Filter out line numbering possibly present in the document. This can be frequent for
     * document in a review/submission format and degrades strongly the machine learning
     * extraction results. 
	 *
	 * -> Not used !
     *
     * @param doc a document
     * @return if found numbering
     */
    /*public boolean filterLineNumber(Document doc) {
        // we first test if we have a line numbering by checking if we have an increasing integer
        // at the begin or the end of each block
        boolean numberBeginLine = false;
        boolean numberEndLine = false;

        boolean foundNumbering = false;

        int currentNumber = -1;
        int lastNumber = -1;
        int i = 0;
        for (Block block : doc.getBlocks()) {
//            Integer ii = i;

            String localText = block.getText();
            List<LayoutToken> tokens = block.getTokens();

            if ((localText != null) && (tokens != null)) {
                if (tokens.size() > 0) {
                    // we get the first and last token iof the block
                    //String tok1 = tokens.get(0).getText();
                    //String tok2 = tokens.get(tokens.size()).getText();
                    localText = localText.trim();

                    Matcher ma1 = startNum.matcher(localText);
                    Matcher ma2 = endNum.matcher(localText);

                    if (ma1.find()) {
                        String groupStr = ma1.group(0);
                        try {
                            currentNumber = Integer.parseInt(groupStr);
                            numberBeginLine = true;
                        } catch (NumberFormatException e) {
                            currentNumber = -1;
                        }
                    } else if (ma2.find()) {
                        String groupStr = ma2.group(0);
                        try {
                            currentNumber = Integer.parseInt(groupStr);
                            numberEndLine = true;
                        } catch (NumberFormatException e) {
                            currentNumber = -1;
                        }
                    }

                    if (lastNumber != -1) {
                        if (currentNumber == lastNumber + 1) {
                            foundNumbering = true;
                            break;
                        }
                    } else
                        lastNumber = currentNumber;
                }
            }
            i++;

            if (i > 5) {
                break;
            }
        }

        i = 0;
        if (foundNumbering) {
            // we have a line numbering, so we filter them
            int counter = 1; // we start at 1, if the actual start is 0,
            // it will remain (as it is negligeable)

            for (Block block : doc.getBlocks()) {

                String localText = block.getText();
                List<LayoutToken> tokens = block.getTokens();

                if ((localText != null) && (tokens.size() > 0)) {

                    if (numberEndLine) {
                        Matcher ma2 = endNum.matcher(localText);

                        if (ma2.find()) {
                            String groupStr = ma2.group(0);
                            if (groupStr.trim().equals("" + counter)) {
                                localText = localText.substring(0, localText.length() - groupStr.length());
                                block.setText(localText);
                                tokens.remove(tokens.size() - 1);
                                counter++;
                            }
                        }

                    } else if (numberBeginLine) {
                        Matcher ma1 = endNum.matcher(localText);

                        if (ma1.find()) {
                            String groupStr = ma1.group(0);
                            if (groupStr.trim().equals("" + counter)) {
                                localText = localText.substring(groupStr.length(), localText.length() - 1);
                                block.setText(localText);
                                tokens.remove(0);
                                counter++;
                            }
                        }

                    }
                }
                i++;
            }
        }

        return foundNumbering;
    }*/

    /**
     * Cluster the blocks following the font, style and size aspects
     *
	 * -> not used at this stage, but could be an interesting feature in the full text model in the future 
	 *
     * @param b   integer
     * @param doc a document
     */
    private static void addBlockToCluster(Integer b, Document doc) {
        // get block features
        Block block = doc.getBlocks().get(b);
        String font = block.getFont();
        boolean bold = block.getBold();
        boolean italic = block.getItalic();
        double fontSize = block.getFontSize();
        boolean found = false;

        if (font == null) {
            font = "unknown";
        }
        //System.out.println(font + " " + bold + " " + italic + " " + fontSize );

        if (doc.getClusters() == null) {
            doc.setClusters(new ArrayList<Cluster>());
        } else {
            for (Cluster cluster : doc.getClusters()) {
                String font2 = cluster.getFont();
                if (font2 == null)
                    font2 = "unknown";
                if (font.equals(font2) &&
                        (bold == cluster.getBold()) &
                                (italic == cluster.getItalic()) &
                                (fontSize == cluster.getFontSize())) {
                    cluster.addBlock2(b);
                    found = true;
                }
            }
        }

        if (!found) {
            Cluster cluster = new Cluster();
            cluster.setFont(font);
            cluster.setBold(bold);
            cluster.setItalic(italic);
            cluster.setFontSize(fontSize);
            cluster.addBlock2(b);
            doc.getClusters().add(cluster);
        }

    }	
	

    static public Document generalResultSegmentation(Document doc, String labeledResult, List<LayoutToken> documentTokens) {
        List<Pair<String, String>> labeledTokens = GenericTaggerUtils.getTokensAndLabels(labeledResult);

        SortedSetMultimap<String, DocumentPiece> labeledBlocks = TreeMultimap.create();
        doc.setLabeledBlocks(labeledBlocks);

        List<Block> docBlocks = doc.getBlocks();
		int indexLine = 0;		
        int blockIndex = 0;
        int p = 0; // position in the labeled result 
		int currentLineEndPos = 0; // position in the global doc. tokenization of the last 
								// token of the current line
		int currentLineStartPos = 0; // position in the global doc. 
									 // tokenization of the first token of the current line
		String line = null;
		
		//DocumentPointer pointerA = DocumentPointer.START_DOCUMENT_POINTER;
		// the default first block might not contain tokens but only bitmap - in this case we move
		// to the first block containing some LayoutToken objects
        while (docBlocks.get(blockIndex).getTokens() == null
                //TODO: make things right
//                || docBlocks.get(blockIndex).getStartToken() == -1
                ) {
            blockIndex++;
        }
        DocumentPointer pointerA = new DocumentPointer(doc, blockIndex, docBlocks.get(blockIndex).getStartToken());
		
        DocumentPointer currentPointer = null;
        DocumentPointer lastPointer = null;

        String curLabel;
        String curPlainLabel = null;
        String lastPlainLabel = null;

        int lastTokenInd = -1;
        for (int i = docBlocks.size() - 1; i >=0; i--) {
            int endToken = docBlocks.get(i).getEndToken();
            if (endToken != -1) {
                lastTokenInd = endToken;
                break;
            }
        }

        // we do this concatenation trick so that we don't have to process stuff after the main loop
        // no copying of lists happens because of this, so it's ok to concatenate
        String ignoredLabel = "@IGNORED_LABEL@";
        for (Pair<String, String> labeledTokenPair :
                Iterables.concat(labeledTokens, 
					Collections.singleton(Pair.of("IgnoredToken", ignoredLabel)))) {
            if (labeledTokenPair == null) {
                p++;
                continue;
            }

			// as we process the document segmentation line by line, we don't use the usual 
			// tokenization to rebuild the text flow, but we get each line again from the 
			// text stored in the document blocks (similarly as when generating the features) 
			line = null;
			while( (line == null) && (blockIndex < docBlocks.size()) ) {
				Block block = docBlocks.get(blockIndex);
		        List<LayoutToken> tokens = block.getTokens();
				String localText = block.getText();
		        if ( (tokens == null) || (localText == null) || (localText.trim().length() == 0) ) {
					blockIndex++;
					indexLine = 0;
					if (blockIndex < docBlocks.size()) {
						block = docBlocks.get(blockIndex);
						currentLineStartPos = block.getStartToken();
					}
		            continue;
		        }
				String[] lines = localText.split("[\\n\\r]");
				if ( (lines.length == 0) || (indexLine >= lines.length)) {
					blockIndex++;
					indexLine = 0;
					if (blockIndex < docBlocks.size()) {
						block = docBlocks.get(blockIndex);
						currentLineStartPos = block.getStartToken();
					}
					continue;
				}
				else {
					line = lines[indexLine];
					indexLine++;
					if ( (line.trim().length() == 0) || (TextUtilities.filterLine(line)) ) {
						line = null;
						continue;
					}

					if (currentLineStartPos > lastTokenInd)
						continue;
					
					// adjust the start token position in documentTokens to this non trivial line
					// first skip possible space characters and tabs at the beginning of the line
					while( (documentTokens.get(currentLineStartPos).t().equals(" ") ||
							documentTokens.get(currentLineStartPos).t().equals("\t") )
					 	&& (currentLineStartPos != lastTokenInd)) {
					 	currentLineStartPos++;
					}
					if (!labeledTokenPair.getLeft().startsWith(documentTokens.get(currentLineStartPos).getText())) {
						while(currentLineStartPos < block.getEndToken()) {
							if (documentTokens.get(currentLineStartPos).t().equals("\n")
							 || documentTokens.get(currentLineStartPos).t().equals("\r")) {
								 // move to the start of the next line, but ignore space characters and tabs
								 currentLineStartPos++;
								 while( (documentTokens.get(currentLineStartPos).t().equals(" ") ||
									 	documentTokens.get(currentLineStartPos).t().equals("\t") )
								 	&& (currentLineStartPos != lastTokenInd)) {
								 	currentLineStartPos++;
								 }
								 if ((currentLineStartPos != lastTokenInd) && 
								 	labeledTokenPair.getLeft().startsWith(documentTokens.get(currentLineStartPos).getText())) {
									 break;
								 }
							 }
							 currentLineStartPos++;
						}
					}

					// what is then the position of the last token of this line?
					currentLineEndPos = currentLineStartPos;
					while(currentLineEndPos < block.getEndToken()) {
						if (documentTokens.get(currentLineEndPos).t().equals("\n")
						 || documentTokens.get(currentLineEndPos).t().equals("\r")) {
							currentLineEndPos--;
							break;
						}
						currentLineEndPos++;
					}
				}
			}
            curLabel = labeledTokenPair.getRight();
            curPlainLabel = GenericTaggerUtils.getPlainLabel(curLabel);
			
			/*System.out.println("-------------------------------");
			System.out.println("block: " + blockIndex);
			System.out.println("line: " + line);
			System.out.println("token: " + labeledTokenPair.a);
			System.out.println("curPlainLabel: " + curPlainLabel);
			System.out.println("lastPlainLabel: " + lastPlainLabel);
			if ((currentLineStartPos < lastTokenInd) && (currentLineStartPos != -1))
				System.out.println("currentLineStartPos: " + currentLineStartPos + 
											" (" + documentTokens.get(currentLineStartPos) + ")");
			if ((currentLineEndPos < lastTokenInd) && (currentLineEndPos != -1))
				System.out.println("currentLineEndPos: " + currentLineEndPos + 
											" (" + documentTokens.get(currentLineEndPos) + ")");*/
			
			if (blockIndex == docBlocks.size()) {
				break;
			}

            currentPointer = new DocumentPointer(doc, blockIndex, currentLineEndPos);

            // either a new entity starts or a new beginning of the same type of entity
			if ((!curPlainLabel.equals(lastPlainLabel)) && (lastPlainLabel != null)) {	
				if ( (pointerA.getTokenDocPos() <= lastPointer.getTokenDocPos()) &&
				   	(pointerA.getTokenDocPos() != -1) ) {
					labeledBlocks.put(lastPlainLabel, new DocumentPiece(pointerA, lastPointer));
				}
                pointerA = new DocumentPointer(doc, blockIndex, currentLineStartPos);
				//System.out.println("add segment for: " + lastPlainLabel + ", until " + (currentLineStartPos-2));
            }

            //updating stuff for next iteration
            lastPlainLabel = curPlainLabel;
            lastPointer = currentPointer;
			currentLineStartPos = currentLineEndPos+2; // one shift for the EOL, one for the next line
            p++;
        }
		
		if (blockIndex == docBlocks.size()) {
			// the last labelled piece has still to be added
			if ((!curPlainLabel.equals(lastPlainLabel)) && (lastPlainLabel != null)) {	
				if ( (pointerA.getTokenDocPos() <= lastPointer.getTokenDocPos()) && 
					(pointerA.getTokenDocPos() != -1) ) {
					labeledBlocks.put(lastPlainLabel, new DocumentPiece(pointerA, lastPointer));
					//System.out.println("add segment for: " + lastPlainLabel + ", until " + (currentLineStartPos-2));
				}
			}
		}

        return doc;
    }

    /**
     * Set the main segments of the document based on the full text parsing results
     *
     * @param doc           a document
     * @param labeledResult string
     * @param tokenizations tokens
     * @return a document
     */
    static public Document resultSegmentation(Document doc, String labeledResult, List<String> tokenizations) {
        if (doc == null) {
            throw new NullPointerException("Document is null");
        }
        if (doc.getBlocks() == null) {
            throw new NullPointerException("Blocks of the documents are null");
        }
        //System.out.println(tokenizations.toString());
//        int i = 0;
//        boolean first = true;
        List<Integer> blockHeaders = new ArrayList<Integer>();
        List<Integer> blockFooters = new ArrayList<Integer>();
        List<Integer> blockDocumentHeaders = new ArrayList<Integer>();
        List<Integer> blockSectionTitles = new ArrayList<Integer>();

        SortedSet<DocumentPiece> blockReferences = new TreeSet<DocumentPiece>();

        doc.setBibDataSets(new ArrayList<BibDataSet>());

//        StringTokenizer st = new StringTokenizer(labeledResult, "\n");

        String[] lines = labeledResult.split("\n");

        String currentTag = null;
        String s2 = null;
        String lastTag = null;
        String lastPlainTag = null;

        int p = 0; // index in the results' tokenization (st)
        int blockIndex = 0;

        BibDataSet bib = null;

        DocumentPointer pointerA = null;
//        DocumentPointer pointerB = null;
        DocumentPointer currentPointer;
        DocumentPointer lastPointer = null;


        for (String line : lines) {
//        while (st.hasMoreTokens()) {

            for (; blockIndex < doc.getBlocks().size() - 1; blockIndex++) {
//                int startTok = doc.getBlocks().get(blockIndex).getStartToken();
                int endTok = doc.getBlocks().get(blockIndex).getEndToken();

                if (endTok >= p) {
                    break;
                }
            }

            ArrayList<String> localFeatures = new ArrayList<String>();
            boolean addSpace = false;

//            String tok = st.nextToken().trim();
            line = line.trim();

            StringTokenizer stt = new StringTokenizer(line, "\t");
            int j = 0;

            boolean newLine = false;
            int ll = stt.countTokens();
            while (stt.hasMoreTokens()) {
                String s = stt.nextToken().trim();
                if (j == 0) {
                    s2 = s;
                    boolean strop = false;
                    while ((!strop) && (p < tokenizations.size())) {
                        String tokOriginal = tokenizations.get(p);
                        if (tokOriginal.equals(" ")
                                | tokOriginal.equals("\n")
                                | tokOriginal.equals("\r")
                                | tokOriginal.equals("\t")) {
                            addSpace = true;
                            p++;
                        } else if (tokOriginal.equals("")) {
                            p++;
                        } else //if (tokOriginal.equals(s))
                        {
                            strop = true;
                        }

                    }
                } else if (j == ll - 1) {
                    currentTag = s; // current tag
                } else {
                    if (s.equals("LINESTART")) {
                        newLine = true;
                    }
                    localFeatures.add(s);
                }
                j++;
            }

            if (lastTag != null) {
                if (lastTag.startsWith("I-")) {
                    lastPlainTag = lastTag.substring(2, lastTag.length());
                } else {
                    lastPlainTag = lastTag;
                }
            }


            String currentPlainTag = null;
            if (currentTag != null) {
                if (currentTag.startsWith("I-")) {
                    currentPlainTag = currentTag.substring(2, currentTag.length());
                } else {
                    currentPlainTag = currentTag;
                }
            }


            currentPointer = new DocumentPointer(doc, blockIndex, p);


            if (lastPlainTag != null && !currentPlainTag.equals(lastPlainTag) && lastPlainTag.equals("<references>")) {
                blockReferences.add(new DocumentPiece(pointerA, lastPointer));
                pointerA = currentPointer;
            }

            if (currentPlainTag.equals("<header>")) {
                if (!blockDocumentHeaders.contains(blockIndex)) {
                    blockDocumentHeaders.add(blockIndex);
                    //System.out.println("add block header: " + blockIndexInteger.intValue());
                }

            } else if (currentPlainTag.equals("<references>")) {//                    if (!blockReferences.contains(blockIndex)) {
//                        blockReferences.add(blockIndex);
//                        //System.out.println("add block reference: " + blockIndexInteger.intValue());
//                    }

                if (currentTag.equals("I-<references>")) {
                    pointerA = new DocumentPointer(doc, blockIndex, p);
                    if (bib != null) {
                        if (bib.getRawBib() != null) {
                            doc.getBibDataSets().add(bib);
                            bib = new BibDataSet();
                        }
                    } else {
                        bib = new BibDataSet();
                    }
                    bib.setRawBib(s2);
                } else {
                    if (addSpace) {
                        if (bib == null) {
                            bib = new BibDataSet();
                            bib.setRawBib(" " + s2);
                        } else {
                            bib.setRawBib(bib.getRawBib() + " " + s2);
                        }
                    } else {
                        if (bib == null) {
                            bib = new BibDataSet();
                            bib.setRawBib(s2);
                        } else {
                            bib.setRawBib(bib.getRawBib() + s2);
                        }
                    }
                }

//                case "<reference_marker>":
//                    if (!blockReferences.contains(blockIndex)) {
//                        blockReferences.add(blockIndex);
//                        //System.out.println("add block reference: " + blockIndexInteger.intValue());
//                    }
//
//                    if (currentTag.equals("I-<reference_marker>")) {
//                        if (bib != null) {
//                            if (bib.getRefSymbol() != null) {
//                                doc.getBibDataSets().add(bib);
//                                bib = new BibDataSet();
//                            }
//                        } else {
//                            bib = new BibDataSet();
//                        }
//                        bib.setRefSymbol(s2);
//                    } else {
//                        if (addSpace) {
//                            if (bib == null) {
//                                bib = new BibDataSet();
//                                bib.setRefSymbol(s2);
//                            } else {
//                                bib.setRefSymbol(bib.getRefSymbol() + " " + s2);
//                            }
//                        } else {
//                            if (bib == null) {
//                                bib = new BibDataSet();
//                                bib.setRefSymbol(s2);
//                            } else {
//                                bib.setRefSymbol(bib.getRefSymbol() + s2);
//                            }
//                        }
//                    }
//                    break;
            } else if (currentPlainTag.equals("<page_footnote>")) {
                if (!blockFooters.contains(blockIndex)) {
                    blockFooters.add(blockIndex);
                    //System.out.println("add block foot note: " + blockIndexInteger.intValue());
                }

            } else if (currentPlainTag.equals("<page_header>")) {
                if (!blockHeaders.contains(blockIndex)) {
                    blockHeaders.add(blockIndex);
                    //System.out.println("add block page header: " + blockIndexInteger.intValue());
                }

            } else if (currentPlainTag.equals("<section>")) {
                if (!blockSectionTitles.contains(blockIndex)) {
                    blockSectionTitles.add(blockIndex);
                    //System.out.println("add block page header: " + blockIndexInteger.intValue());
                }

            }

            lastTag = currentTag;
            p++;
            lastPointer = currentPointer;
        }

        if (bib != null) {
            doc.getBibDataSets().add(bib);
        }


        if (!lastPointer.equals(pointerA)) {
            if (lastPlainTag.equals("<references>")) {
                blockReferences.add(new DocumentPiece(pointerA, lastPointer));
            }
        }

        /*doc.setBlockHeaders(blockHeaders);
        doc.setBlockFooters(blockFooters);
        doc.setBlockDocumentHeaders(blockDocumentHeaders);
        doc.setBlockReferences(blockReferences);
        doc.setBlockSectionTitles(blockSectionTitles);*/

        return doc;
    }

}