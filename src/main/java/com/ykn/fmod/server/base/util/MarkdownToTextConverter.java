/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * This class provides methods to analyze, parse, and convert
 * markdown syntax into structured text representations. It supports identifying various
 * markdown elements such as headers, bold, italic, inline code, hyperlinks, and more.
 * Additionally, it includes functionality for syntax highlighting of code blocks in
 * supported programming languages like Java, C++, and Python.
 */
public class MarkdownToTextConverter {

    /**
     * Checks if a given number falls within any of the ranges specified by the start and end lists.
     *
     * @param start a list of integers representing the start of each range (inclusive)
     * @param end a list of integers representing the end of each range (inclusive)
     * @param number the number to check
     * @return true if the number is within any of the specified ranges, false otherwise
     */
    public static boolean isNumberBetween(List<Integer> start, List<Integer> end, int number) {
        for (int i = 0; i < start.size(); i++) {
            if (start.get(i) <= number && number <= end.get(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Analyzes the given markdown string and identifies various markdown syntax elements.
     * The identified elements are stored in the provided lists.
     *
     * @param markdown The markdown string to analyze.
     * @param unitStartIndex A list to store the start indices of the identified markdown units.
     * @param unitEndIndex A list to store the end indices of the identified markdown units.
     * @param unitType A list to store the types of the identified markdown units.
     * @param unitText A list to store the text content of the identified markdown units.
     * @param unitHint A list to store additional hints or metadata for the identified markdown units.
     */
    public static void analyzeMarkdownSyntax(String markdown, 
                                            ArrayList<Integer> unitStartIndex, 
                                            ArrayList<Integer> unitEndIndex, 
                                            ArrayList<MarkdownUnit> unitType, 
                                            ArrayList<String> unitText, 
                                            ArrayList<String> unitHint) {
        Pattern codePattern = Pattern.compile("(?m)^( *)(`{3,})(\\w*)\n([\\s\\S]*?)\\n( *)\\2", Pattern.MULTILINE); // Group = 4
        Pattern mathPattern = Pattern.compile("(?<!\\\\)\\$(.*?)(?<!\\\\)\\$"); // Group = 1
        Pattern headerPattern = Pattern.compile("(?m)^( *)(#{1,5}) +(.*)"); // Group = 3
        Pattern boldPattern = Pattern.compile("(?<!\\\\)\\*\\*(?!\\s)(.*?)(?<!\\s)(?<!\\\\)\\*\\*"); // Group = 1
        // Pattern italicPattern = Pattern.compile("(?<!\\\\)(?:\\*|_)(?!\\s)(.*?)(?<!\\s)(?<!\\\\)(?:\\*|_)"); // Group = 1
        Pattern italicPattern = Pattern.compile("(?<!\\\\)(?:\\*)(?!\\s)(.*?)(?<!\\s)(?<!\\\\)(?:\\*)"); // Disable _ italic because it may cause too many false positive
        Pattern strikePattern = Pattern.compile("(?<!\\\\)~~(?!\\s)(.*?)(?<!\\s)(?<!\\\\)~~"); // Group = 1
        Pattern inlinePattern = Pattern.compile("(?<!\\\\)`(.*?)(?<!\\\\)`"); // Group = 1
        Pattern enumPattern = Pattern.compile("(?m)^( *)(- )+(.*)"); // Group = 3
        Pattern hyperlinkPattern = Pattern.compile("(?<!\\\\)\\[(.*?)\\]\\((.*?)\\)"); // Group = 1, 2

        // Use \u00a7 to label different parts of the markdown, exisiting \u00a7 will be replaced by \\u00a7
        String parsedStr = markdown.replace("\u00a7", "\\u00a7");
        ArrayList<Integer> codePartStartIndex = new ArrayList<>();
        ArrayList<Integer> codePartEndIndex = new ArrayList<>();
        
        // ArrayList<Integer> unitStartIndex = new ArrayList<>();
        // ArrayList<Integer> unitEndIndex = new ArrayList<>();
        // ArrayList<MarkdownUnit> unitType = new ArrayList<>();
        // ArrayList<String> unitText = new ArrayList<>();
        // ArrayList<String> unitHint = new ArrayList<>();
        unitStartIndex.clear();
        unitEndIndex.clear();
        unitType.clear();
        unitText.clear();
        unitHint.clear();
        
        // match code part
        Matcher codeMatcher = codePattern.matcher(parsedStr);
        while (codeMatcher.find()) {
            codePartStartIndex.add(codeMatcher.start());
            codePartEndIndex.add(codeMatcher.end());
            unitStartIndex.add(codeMatcher.start());
            unitEndIndex.add(codeMatcher.end());
            unitType.add(MarkdownUnit.CODE);
            unitText.add(codeMatcher.group(4));
            unitHint.add(codeMatcher.group(3)); // Language
        }
        // match math part outside code part and remove $ symbol
        Matcher mathMatcher = mathPattern.matcher(parsedStr);
        while (mathMatcher.find()) {
            int startIndex = mathMatcher.start();
            int endIndex = mathMatcher.end();
            String mathContent = mathMatcher.group(1);
            if (mathContent.length() > 0 && isNumberBetween(codePartStartIndex, codePartEndIndex, startIndex) == false && isNumberBetween(codePartStartIndex, codePartEndIndex, endIndex) == false) {
                unitStartIndex.add(startIndex);
                unitEndIndex.add(endIndex);
                unitType.add(MarkdownUnit.MATH);
                unitText.add(mathContent);
                unitHint.add("");
            }
        }
        // match header part
        Matcher headerMatcher = headerPattern.matcher(parsedStr);
        while (headerMatcher.find()) {
            int startIndex = headerMatcher.start();
            int endIndex = headerMatcher.end();
            String headerContent = headerMatcher.group(3);
            if (headerContent.length() > 0 && isNumberBetween(codePartStartIndex, codePartEndIndex, startIndex) == false && isNumberBetween(codePartStartIndex, codePartEndIndex, endIndex) == false) {
                unitStartIndex.add(startIndex);
                unitEndIndex.add(endIndex);
                unitType.add(MarkdownUnit.HEADER);
                unitText.add(headerContent);
                unitHint.add(headerMatcher.group(2)); // Number of #
            }
        }
        // match bold part
        Matcher boldMatcher = boldPattern.matcher(parsedStr);
        while (boldMatcher.find()) {
            int startIndex = boldMatcher.start();
            int endIndex = boldMatcher.end();
            String boldContent = boldMatcher.group(1);
            if (boldContent.length() > 0 && isNumberBetween(codePartStartIndex, codePartEndIndex, startIndex) == false && isNumberBetween(codePartStartIndex, codePartEndIndex, endIndex) == false) {
                unitStartIndex.add(startIndex);
                unitEndIndex.add(endIndex);
                unitType.add(MarkdownUnit.BOLD);
                unitText.add(boldContent);
                unitHint.add("");
            }
        }
        // match italic part
        Matcher italicMatcher = italicPattern.matcher(parsedStr);
        while (italicMatcher.find()) {
            int startIndex = italicMatcher.start();
            int endIndex = italicMatcher.end();
            String italicContent = italicMatcher.group(1);
            if (italicContent.length() > 0 && isNumberBetween(codePartStartIndex, codePartEndIndex, startIndex) == false && isNumberBetween(codePartStartIndex, codePartEndIndex, endIndex) == false) {
                unitStartIndex.add(startIndex);
                unitEndIndex.add(endIndex);
                unitType.add(MarkdownUnit.ITALIC);
                unitText.add(italicContent);
                unitHint.add("");
            }
        }
        // match strike part
        Matcher strikeMatcher = strikePattern.matcher(parsedStr);
        while (strikeMatcher.find()) {
            int startIndex = strikeMatcher.start();
            int endIndex = strikeMatcher.end();
            String strikeContent = strikeMatcher.group(1);
            if (strikeContent.length() > 0 && isNumberBetween(codePartStartIndex, codePartEndIndex, startIndex) == false && isNumberBetween(codePartStartIndex, codePartEndIndex, endIndex) == false) {
                unitStartIndex.add(startIndex);
                unitEndIndex.add(endIndex);
                unitType.add(MarkdownUnit.STRIKE);
                unitText.add(strikeContent);
                unitHint.add("");
            }
        }
        // match inline part
        Matcher inlineMatcher = inlinePattern.matcher(parsedStr);
        while (inlineMatcher.find()) {
            int startIndex = inlineMatcher.start();
            int endIndex = inlineMatcher.end();
            String inlineContent = inlineMatcher.group(1);
            if (inlineContent.length() > 0 && isNumberBetween(codePartStartIndex, codePartEndIndex, startIndex) == false && isNumberBetween(codePartStartIndex, codePartEndIndex, endIndex) == false) {
                unitStartIndex.add(startIndex);
                unitEndIndex.add(endIndex);
                unitType.add(MarkdownUnit.INLINE);
                unitText.add(inlineContent);
                unitHint.add("");
            }
        }
        // match enum part
        Matcher enumMatcher = enumPattern.matcher(parsedStr);
        while (enumMatcher.find()) {
            int startIndex = enumMatcher.start();
            int endIndex = enumMatcher.end();
            String enumContent = enumMatcher.group(3);
            if (enumContent.length() > 0 && isNumberBetween(codePartStartIndex, codePartEndIndex, startIndex) == false && isNumberBetween(codePartStartIndex, codePartEndIndex, endIndex) == false) {
                unitStartIndex.add(startIndex);
                unitEndIndex.add(endIndex);
                unitType.add(MarkdownUnit.ENUM);
                unitText.add(enumContent);
                unitHint.add(enumMatcher.group(2)); // Number of -
            }
        }
        // match hyperlink part
        Matcher hyperlinkMatcher = hyperlinkPattern.matcher(parsedStr);
        while (hyperlinkMatcher.find()) {
            int startIndex = hyperlinkMatcher.start();
            int endIndex = hyperlinkMatcher.end();
            String hyperlinkContent = hyperlinkMatcher.group(1);
            String hyperlinkUrl = hyperlinkMatcher.group(2);
            if (hyperlinkContent.length() > 0 && hyperlinkUrl.length() > 0 && isNumberBetween(codePartStartIndex, codePartEndIndex, startIndex) == false && isNumberBetween(codePartStartIndex, codePartEndIndex, endIndex) == false) {
                unitStartIndex.add(startIndex);
                unitEndIndex.add(endIndex);
                unitType.add(MarkdownUnit.HYPERLINK);
                unitText.add(hyperlinkContent);
                unitHint.add(hyperlinkUrl); // URL
            }
        }
    }

    /**
     * Converts markdown text into tokens based on visibility and markdown units.
     *
     * @param text The markdown text to be converted.
     * @param unitStartIndex List of start indices for each markdown unit.
     * @param unitEndIndex List of end indices for each markdown unit.
     * @param unitType List of markdown unit types.
     * @param unitText List of visible text for each markdown unit.
     * @param unitHint List of hints for each markdown unit.
     * @param tokenText List to store the resulting token texts.
     * @param tokenType List to store the resulting token types.
     * @param tokenHint List to store the resulting token hints.
     */
    public static void convertTokens(String text,
                                    ArrayList<Integer> unitStartIndex, 
                                    ArrayList<Integer> unitEndIndex, 
                                    ArrayList<MarkdownUnit> unitType, 
                                    ArrayList<String> unitText, 
                                    ArrayList<String> unitHint,
                                    ArrayList<String> tokenText,
                                    ArrayList<ArrayList<MarkdownUnit>> tokenType,
                                    ArrayList<ArrayList<String>> tokenHint) {
        int textLength = text.length();
        int unitCount = unitStartIndex.size();
        // Save the position of the text that need to be displayed in each unit
        int[] visStart = new int[unitCount];
        int[] visEnd = new int[unitCount];

        // Label each char in markdown should be shown or not, init with true
        boolean[] isVisible = new boolean[textLength];
        Arrays.fill(isVisible, true);

        // For each unit, calculate which char should be shown
        for (int i = 0; i < unitCount; i++) {
            int uStart = unitStartIndex.get(i);
            int uEnd = unitEndIndex.get(i);
            String full = text.substring(uStart, uEnd);  // including not visible char
            String visiblePart = unitText.get(i); // only visible char

            int a = full.indexOf(visiblePart);  // start of visible part in full
            int b = full.length() - a - visiblePart.length(); // end of visible part in full

            visStart[i] = uStart + a;
            visEnd[i] = uEnd - b;  

            // Set invisible char to false
            for (int j = uStart; j < uStart + a; j++) {
                isVisible[j] = false;
            }
            for (int j = uEnd - b; j < uEnd; j++) {
                isVisible[j] = false;
            }
        }

        // Split the markdown into tokens
        int pos = 0;
        while (pos < textLength) {
            if (!isVisible[pos]) {
                pos++;
                continue;
            }
            // Find a list of char that should be shown, and they must have the same markdown type
            int tokenStartPos = pos;
            LinkedHashSet<MarkdownUnit> currUnitSet = new LinkedHashSet<>();
            LinkedHashSet<String> currHintSet = new LinkedHashSet<>();
            for (int u = 0; u < unitCount; u++) {
                if (pos >= visStart[u] && pos < visEnd[u]) {
                    currUnitSet.add(unitType.get(u));
                    currHintSet.add(unitHint.get(u));
                }
            }

            // Find the end of the token, until the end of the string or the markdown type changes
            int endPos = pos;
            while (endPos < textLength && isVisible[endPos]) {
                LinkedHashSet<MarkdownUnit> unitSet = new LinkedHashSet<>();
                LinkedHashSet<String> hintSet = new LinkedHashSet<>();
                for (int u = 0; u < unitCount; u++) {
                    if (endPos >= visStart[u] && endPos < visEnd[u]) {
                        unitSet.add(unitType.get(u));
                        hintSet.add(unitHint.get(u));
                    }
                }

                // End of the token if the markdown type changes
                if (!unitSet.equals(currUnitSet) || !hintSet.equals(currHintSet)) {
                    break;
                }

                endPos++;
            }

            // Build the token
            StringBuilder sb = new StringBuilder();
            for (int k = tokenStartPos; k < endPos; k++) {
                sb.append(text.charAt(k));
            }
            String tokenStr = sb.toString();
            if (!tokenStr.isEmpty()) {
                tokenText.add(tokenStr);
                tokenType.add(new ArrayList<>(currUnitSet));
                tokenHint.add(new ArrayList<>(currHintSet));
            }
            pos = endPos;
        }
    }

    private static MutableComponent parseMarkdownTokenToText(ArrayList<String> tokenText, ArrayList<ArrayList<MarkdownUnit>> tokenType, ArrayList<ArrayList<String>> tokenHint) {
        MutableComponent result = Component.empty();
        int tokenCount = tokenText.size();

        for (int i = 0; i < tokenCount; i++) {
            String token = tokenText.get(i);
            ArrayList<MarkdownUnit> type = tokenType.get(i);
            ArrayList<String> hint = tokenHint.get(i);
            MutableComponent text = Component.literal(token);

            boolean alreadyApplyColor = false;
            boolean alreadyApplyBold = false;
            boolean alreadyApplyItalic = false;
            boolean alreadyApplyStrike = false;
            if (type.size() == 0) {
                result.append(text.withStyle(ChatFormatting.RESET));
                continue;
            }
            if (type.contains(MarkdownUnit.CODE)) {
                result.append(processCodeBlock(hint.get(type.indexOf(MarkdownUnit.CODE)), token));
                continue;   // Code block should not apply other markdown syntax
            }
            if (type.contains(MarkdownUnit.MATH)) {
                text = text.withStyle(ChatFormatting.DARK_RED).withStyle(s -> s
                    .withClickEvent(new ClickEvent(
                        ClickEvent.Action.COPY_TO_CLIPBOARD, 
                        token
                    ))
                    .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Util.parseTranslatableText("fmod.misc.copyto").withStyle(ChatFormatting.GREEN)
                    ))
                );
                alreadyApplyColor = true;
            }
            if (type.contains(MarkdownUnit.BOLD)) {
                if (alreadyApplyBold == false) {
                    text = text.withStyle(ChatFormatting.BOLD);
                    alreadyApplyBold = true;
                }
            }
            if (type.contains(MarkdownUnit.ITALIC)) {
                if (alreadyApplyItalic == false) {
                    text = text.withStyle(ChatFormatting.ITALIC);
                    alreadyApplyItalic = true;
                }
            }
            if (type.contains(MarkdownUnit.STRIKE)) {
                if (alreadyApplyStrike == false) {
                    text = text.withStyle(ChatFormatting.STRIKETHROUGH);
                    alreadyApplyStrike = true;
                }
            }
            if (type.contains(MarkdownUnit.INLINE)) {
                if (alreadyApplyColor == false) {
                    text = text.withStyle(ChatFormatting.GOLD);
                    alreadyApplyColor = true;
                }
                text = text.withStyle(s -> s
                    .withClickEvent(new ClickEvent(
                        ClickEvent.Action.COPY_TO_CLIPBOARD, 
                        token
                    ))
                    .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Util.parseTranslatableText("fmod.misc.copyto").withStyle(ChatFormatting.GREEN)
                    ))
                );
            }
            if (type.contains(MarkdownUnit.HEADER)) {
                if (alreadyApplyColor == false) {
                    text = text.withStyle(ChatFormatting.DARK_AQUA);
                    alreadyApplyColor = true;
                } 
                if (alreadyApplyBold == false) {
                    text = text.withStyle(ChatFormatting.BOLD);
                    alreadyApplyBold = true;
                }
            }
            if (type.contains(MarkdownUnit.ENUM)) {
                if (alreadyApplyColor == false) {
                    text = text.withStyle(ChatFormatting.YELLOW);
                    alreadyApplyColor = true;
                }
            }
            if (type.contains(MarkdownUnit.HYPERLINK)) {
                if (alreadyApplyColor == false) {
                    text = text.withStyle(ChatFormatting.AQUA);
                    alreadyApplyColor = true;
                }
                text = text.withStyle(ChatFormatting.UNDERLINE);
                text = text.withStyle(s -> s
                    .withClickEvent(new ClickEvent(
                        ClickEvent.Action.OPEN_URL, 
                        hint.get(type.indexOf(MarkdownUnit.HYPERLINK))
                    ))
                    .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Util.parseTranslatableText("fmod.misc.openurl", Component.literal(hint.get(type.indexOf(MarkdownUnit.HYPERLINK))).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN)
                    ))
                );
            }
            result.append(text);
        }

        return result;
    }

    /**
     * Tokenizes the given text based on the provided patterns and associates styles with each token.
     *
     * @param <T> The type of the styles.
     * @param text The text to be tokenized.
     * @param patterns A list of regex patterns used to identify tokens in the text.
     * @param styles A list of styles corresponding to each pattern.
     * @param tokens An output list where the identified tokens will be stored.
     * @param tokenStyles An output list where the styles for each token will be stored.
     */
    public static <T> void codeTokenlize(String text, ArrayList<Pattern> patterns, ArrayList<T> styles,
                                        ArrayList<String> tokens, ArrayList<ArrayList<T>> tokenStyles) {
        int n = text.length();
        // Create a list that holds a set of styles (of type T) for each character.
        List<Set<T>> styleAt = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            styleAt.add(new HashSet<>());
        }

        // For each pattern, mark its style on all the characters it covers.
        for (int p = 0; p < patterns.size(); p++) {
            Pattern pat = patterns.get(p);
            T style = styles.get(p);
            Matcher matcher = pat.matcher(text);
            while (matcher.find()) {
                for (int i = matcher.start(); i < matcher.end(); i++) {
                    styleAt.get(i).add(style);
                }
            }
        }

        // Group adjacent characters with identical style sets.
        int start = 0;
        while (start < n) {
            Set<T> currentSet = styleAt.get(start);
            int end = start + 1;
            while (end < n && styleAt.get(end).equals(currentSet)) {
                end++;
            }
            // Extract token substring.
            String token = text.substring(start, end);
            tokens.add(token);
            // Convert the set to an ArrayList.
            tokenStyles.add(new ArrayList<>(currentSet));
            start = end;
        }
    }

    /**
     * Converts a given markdown string into a Text object with simple syntax highlighting.
     *
     * @param markdown The input markdown string to be converted.
     * @return A Text object representing the parsed content of the markdown string.
     */
    public static Component parseMarkdownToText(String markdown) {
        MutableComponent result = Component.empty();

        ArrayList<Integer> unitStartIndex = new ArrayList<>();
        ArrayList<Integer> unitEndIndex = new ArrayList<>();
        ArrayList<MarkdownUnit> unitType = new ArrayList<>();
        ArrayList<String> unitText = new ArrayList<>();
        ArrayList<String> unitHint = new ArrayList<>();

        analyzeMarkdownSyntax(markdown, unitStartIndex, unitEndIndex, unitType, unitText, unitHint);

        ArrayList<String> tokenText = new ArrayList<>();
        ArrayList<ArrayList<MarkdownUnit>> tokenType = new ArrayList<>();
        ArrayList<ArrayList<String>> tokenHint = new ArrayList<>();

        convertTokens(markdown, unitStartIndex, unitEndIndex, unitType, unitText, unitHint, tokenText, tokenType, tokenHint);

        result = parseMarkdownTokenToText(tokenText, tokenType, tokenHint);

        return result;
    }

    private static Component processCodeBlock(String lang, String code) {
        MutableComponent codeText = Component.empty();

        if (code == null) {
            return codeText;
        }

        switch (lang.toLowerCase()) {
            case "java":
                codeText = syntaxHighlightJava(code);
                break;
            case "c":
            case "cpp":
                codeText = syntaxHighlightCpp(code);
                break;
            case "python":
                codeText = syntaxHighlightPython(code);
                break;
            default:
                codeText = Component.literal(code).withStyle(ChatFormatting.GRAY);
        }

        return codeText.withStyle(s -> s
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD, 
                    code
                ))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Util.parseTranslatableText("fmod.misc.copyto").withStyle(ChatFormatting.GREEN)
                ))
            );
    }

    private static MutableComponent syntaxHighlightJava(String code) {
        Pattern commentPattern = Pattern.compile("(?s)/\\*.*?\\*/|//.*?(?=\\r?\\n|$)");
        Pattern stringPattern = Pattern.compile("(\"(?:(?:\\\\.)|[^\"\\\\])*?\")|('(?:(?:\\\\.)|[^'\\\\])*')");
        Pattern keywordPattern = Pattern.compile("\\b(?:abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|exports|extends|final|finally|float|for|if|goto|implements|import|instanceof|int|interface|long|module|native|new|package|permits|private|protected|provides|public|record|requires|return|short|static|sealed|strictfp|super|switch|synchronized|this|throw|throws|transient|try|var|void|volatile|while|with|true|false|null)\\b");
        Pattern numberPattern = Pattern.compile("(?<![A-Za-z0-9_$])(0[xXb][0-9A-Fa-f]+(?:[lL])?|(?:\\d+\\.\\d*|\\.\\d+|\\d+)(?:[eE][+-]?\\d+)?(?:[fFdDlL])?)(?![A-Za-z0-9_$])");
        Pattern punctuationPattern = Pattern.compile("[{}()\\[\\];.,<>+\\-*/%&|^!~?:=`\"'@#$\\\\]");
        Pattern classPattern = Pattern.compile("<\\s*[A-Za-z_$][\\w\\s:<>,?]*\\s*>|(?<=\\b(class|extends|implements|interface|instanceof|enum|throws|record|permits)\\s+)[A-Za-z_$]\\w*\\b|\\b[A-Za-z_$]\\w*(?=(\\s*<\\s*[A-Za-z_$]?[\\w\\s:<>,?]*\\s*>)?\\s*::)|(?<![A-Za-z0-9_$](<\\s*[A-Za-z_$]?[\\w\\s:<>,?]*\\s*>)?\\s*)\\(\\s*[A-Za-z_$]\\w*(\\s*<\\s*[A-Za-z_$]?[\\w\\s:<>,?]*\\s*>)?\\s*\\)(?!\\s*(;|->|\\)|\\{|\\[|\\?))|(?<=\\breturn\\s*)\\(\\s*[A-Za-z_$]\\w*(\\s*<\\s*[A-Za-z_$]?[\\w\\s:<>,?]*\\s*>)?\\s*\\)(?!\\s*(;|->|\\)|\\{|\\[|\\?))|\\b[A-Za-z_$]\\w*(?=\\s*(<\\s*[A-Za-z_$]?[\\w\\s:<>,?]*\\s*>)?(\\s*\\[\\s*[0-9]*\\s*\\])?\\s+[A-Za-z_$]\\w*\\b)|(?<=\\b(package|import|exports|module|requires|provides|with)\\s+)(\\w+\\s*\\.\\s*)*\\w+|(?<=(?<![A-Za-z0-9_$]\\s*)@\\s*)[A-Za-z_$]\\w*\\b");
        Pattern functionPattern = Pattern.compile("\\b[A-Za-z_$]\\w*(?=\\s*(<\\s*[A-Za-z_$]?[\\w\\s:<>,?]*\\s*>)?\\s*\\()");
        // May not be able to handle ? symbol in generics, such as List<? extends Number>, and class name with full package path
        
        ArrayList<Pattern> patterns = new ArrayList<>();
        patterns.add(commentPattern);
        patterns.add(stringPattern);
        patterns.add(keywordPattern);
        patterns.add(numberPattern);
        patterns.add(punctuationPattern);
        patterns.add(classPattern);
        patterns.add(functionPattern);

        ArrayList<CodeUnit> codeType = new ArrayList<>();
        codeType.add(CodeUnit.COMMENT);
        codeType.add(CodeUnit.STRING);
        codeType.add(CodeUnit.KEYWORD);
        codeType.add(CodeUnit.NUMBER);
        codeType.add(CodeUnit.PUNCTUATION);
        codeType.add(CodeUnit.CLASS);
        codeType.add(CodeUnit.FUNCTION);

        ArrayList<String> tokens = new ArrayList<>();
        ArrayList<ArrayList<CodeUnit>> tokenTypes = new ArrayList<>();
        codeTokenlize(code, patterns, codeType, tokens, tokenTypes);

        MutableComponent result = Component.empty();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            ArrayList<CodeUnit> type = tokenTypes.get(i);

            if (type.size() == 0) {
                result.append(Component.literal(token).withStyle(ChatFormatting.GRAY));
            } else if (type.contains(CodeUnit.STRING)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.GOLD));
            } else if (type.contains(CodeUnit.COMMENT)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.DARK_GREEN));
            }  else if (type.contains(CodeUnit.KEYWORD)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            } else if (type.contains(CodeUnit.NUMBER)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (type.contains(CodeUnit.PUNCTUATION)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.DARK_AQUA));
            } else if (type.contains(CodeUnit.CLASS)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.GREEN));
            } else if (type.contains(CodeUnit.FUNCTION)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.YELLOW));
            }
        }

        return result;
    }

    private static MutableComponent syntaxHighlightCpp(String code) {
        Pattern commentPattern = Pattern.compile("(?s)/\\*.*?\\*/|//.*?(?=\\r?\\n|$)");
        Pattern stringPattern = Pattern.compile("(\"(?:(?:\\\\.)|[^\"\\\\])*?\")|('(?:(?:\\\\.)|[^'\\\\])*')");
        Pattern keywordPattern = Pattern.compile("\\b(?:alignas|alignof|auto|break|case|catch|class|concept|const|constexpr|continue|decltype|default|delete|do|else|enum|explicit|export|extern|for|final|friend|goto|if|import|inline|module|mutable|namespace|noexcept|new|operator|override|private|protected|public|requires|return|sizeof|static|struct|switch|template|this|throw|try|typedef|typeid|typename|union|using|virtual|volatile|while|int|double|float|long|char|unsigned|signed|void|bool|true|false|nullptr|static_cast|dynamic_cast|const_cast|reinterpret_cast|assert|static_assert|thread_local)\\b");
        Pattern preprocessorPattern = Pattern.compile("(?m)^\\s*#\\s*\\w+");
        Pattern numberPattern = Pattern.compile("(?<![A-Za-z0-9_$])(0[xXb][0-9A-Fa-f]+(?:[lL])?|(?:\\d+\\.\\d*|\\.\\d+|\\d+)(?:[eE][+-]?\\d+)?(?:[fFdDlL])?)(?![A-Za-z0-9_$])");
        Pattern punctuationPattern = Pattern.compile("[{}()\\[\\];.,<>+\\-*/%&|^!~?:=`\"'@#$\\\\]");
        Pattern headerPattern = Pattern.compile("(?m)^\\s*#\\s*include\\s+[<\"]\\S+[>\"]");
        Pattern classPattern = Pattern.compile("<\\s*[A-Za-z_$][\\w\\s:<>,*]*\\s*>|(?<=\\b(class|typename|typedef|enum|using|namespace|struct)\\s+)[A-Za-z_$]\\w*\\b|\\b[A-Za-z_$]\\w*(?=(\\s*<\\s*[A-Za-z_$]?[\\w\\s:<>,*]*\\s*>)?\\s*::)|(?<![A-Za-z0-9_$](<\\s*[A-Za-z_$]?[\\w\\s:<>,*]*\\s*>)?\\s*)\\(\\s*([A-Za-z_$]\\w*\\s*::\\s*)*[A-Za-z_$]\\w*(\\s*<\\s*[A-Za-z_$]?[\\w\\s:<>,*]*\\s*>)?(\\s*(?![&*\\s]*&\\s*&)[&*][&*\\s]*)?\\s*\\)(?!\\s*(;|->|\\)|\\{|\\[|\\?))|(?<=\\breturn\\s*)\\(\\s*([A-Za-z_$]\\w*\\s*::\\s*)*[A-Za-z_$]\\w*(\\s*<\\s*[A-Za-z_$]?[\\w\\s:<>,*]*\\s*>)?(\\s*(?![&*\\s]*&\\s*&)[&*][&*\\s]*)?\\s*\\)(?!\\s*(;|->|\\)|\\{|\\[|\\?))|\\b[A-Za-z_$]\\w*(?=\\s*(<\\s*[A-Za-z_$]?[\\w\\s:<>,*]*\\s*>)?\\s*(\\[\\s*([0-9]*|0[xXb][0-9A-Fa-f]+)[lL]?\\s*\\])?(\\s*(?![&*\\s]*&\\s*&)[&*][&*\\s]*)?\\s+(\\s*(?![&*\\s]*&\\s*&)[&*][&*\\s]*)?[A-Za-z_$]\\w*\\b)|(?<=\\bclass\\s+[A-Za-z_$]\\w*\\s*:[\\w\\s:<>,*]*\\s*)[A-Za-z_$]\\w*\\b");
        Pattern functionPattern = Pattern.compile("\\b[A-Za-z_$]\\w*(?=\\s*(<\\s*[A-Za-z_$]?[\\w\\s:<>,*]*\\s*>)?\\s*\\()|\\(\\s*(\\*\\s*)*[A-Za-z_$]\\w*\\s*(\\[\\s*([0-9]*|0[xXb][0-9A-Fa-f]+)[lL]?\\s*\\]\\s*)?\\)(?=\\s*(<\\s*[A-Za-z_$]?[\\w\\s:<>,*]*\\s*>)?\\s*\\()");  // may not handle nested syntax correctly
        // Commonly Used Patterns:
        // (<\s*[A-Za-z_$]?[\w\s:<>,*]*\s*>)? An optional template syntax after a function or a class name, partially handle nested syntax
        // (\s*(?![&*\s]*&\s*&)[&*][&*\s]*)? An optional pointer or reference syntax while excluding AND && syntax
        // ([A-Za-z_$]\w*\s*::\s*)* One or more optional namespace or class name before a function or a class name
        // Class Pattern: Template | Definition | Namespace | Type Cast | Type Cast | Create Object | Inherit
        // Cannot match newly initialized objects during return, such as "return ArrayList<>()"
        // Cannot match variadic parameters, such as "template<typename... Args>" or "void func(int a, Args... args)"

        ArrayList<Pattern> patterns = new ArrayList<>();
        patterns.add(commentPattern);
        patterns.add(stringPattern);
        patterns.add(keywordPattern);
        patterns.add(preprocessorPattern);
        patterns.add(numberPattern);
        patterns.add(punctuationPattern);
        patterns.add(headerPattern);
        patterns.add(classPattern);
        patterns.add(functionPattern);

        ArrayList<CodeUnit> codeType = new ArrayList<>();
        codeType.add(CodeUnit.COMMENT);
        codeType.add(CodeUnit.STRING);
        codeType.add(CodeUnit.KEYWORD);
        codeType.add(CodeUnit.PREPROCESSOR);
        codeType.add(CodeUnit.NUMBER);
        codeType.add(CodeUnit.PUNCTUATION);
        codeType.add(CodeUnit.HEADER);
        codeType.add(CodeUnit.CLASS);
        codeType.add(CodeUnit.FUNCTION);

        ArrayList<String> tokens = new ArrayList<>();
        ArrayList<ArrayList<CodeUnit>> tokenTypes = new ArrayList<>();
        codeTokenlize(code, patterns, codeType, tokens, tokenTypes);
        
        MutableComponent result = Component.empty();
        int tokenCount = tokens.size();
        for (int i = 0; i < tokenCount; i++) {
            String token = tokens.get(i);
            ArrayList<CodeUnit> type = tokenTypes.get(i);
            // In code block, each part of the text can only have one style, high priority style first
            // For example, string has a higher priority than keyword, because inside a string may contain keywords
            if (type.size() == 0) {
                result.append(Component.literal(token).withStyle(ChatFormatting.GRAY));
            } else if (type.contains(CodeUnit.STRING)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.GOLD));
            } else if (type.contains(CodeUnit.COMMENT)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.DARK_GREEN));
            } else if (type.contains(CodeUnit.PREPROCESSOR)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.DARK_GRAY));
            } else if (type.contains(CodeUnit.KEYWORD)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            } else if (type.contains(CodeUnit.HEADER)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.RED));
            } else if (type.contains(CodeUnit.NUMBER)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (type.contains(CodeUnit.PUNCTUATION)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.DARK_AQUA));
            }  else if (type.contains(CodeUnit.CLASS)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.GREEN));
            } else if (type.contains(CodeUnit.FUNCTION)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.YELLOW));
            }
        }

        return result;
    }

    private static MutableComponent syntaxHighlightPython(String code) {
        Pattern commentPattern = Pattern.compile("#.*");
        Pattern stringPattern = Pattern.compile("\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|(\"(?:(?:\\\\.)|[^\"\\\\])*?\")|('(?:(?:\\\\.)|[^'\\\\])*')");
        Pattern keywordPattern = Pattern.compile("\\b(?:and|as|assert|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|not|or|pass|raise|return|try|while|with|yield|True|False|None)\\b");
        Pattern numberPattern = Pattern.compile("(?<![A-Za-z0-9_$])(0[xXb][0-9A-Fa-f]+|(?:\\d+\\.\\d*|\\.\\d+|\\d+)(?:[eE][+-]?\\d+)?)(?![A-Za-z0-9_$])");
        Pattern punctuationPattern = Pattern.compile("[{}()\\[\\];.,<>+\\-*/%&|^!~?:=`\"'@#$\\\\]");
        Pattern classPattern = Pattern.compile("(?<=\\b(class|from)\\s+)[A-Za-z_$][\\w.]*\\b|(?<=\\bimport\\s+)[A-Za-z_$][\\w.]*\\s+as\\s\\s*[A-Za-z_$]\\w*\\b|(?<=\\b(?<!from\\s+[A-Za-z_$][\\w.]*\\s+)import\\s+)[A-Za-z_$]\\w*\\b");
        Pattern functionPattern = Pattern.compile("(?<=\\bdef\\s+)[A-Za-z_$]\\w*\\s*(?=\\()|(?m)^\\s*@\\s*\\w+\\s*\\b");
        // Python has a very flexible syntax, so it is hard to use regex to match all cases
        // We just give up to match the class name in def fun(a: str, b: tuple = (0, (1, 2)), c) -> str: pass since it is too complex
        // A possible regex for the above syntax:
        // (?<=\bdef\s+[A-Za-z_$]\w*\s*\([\w\s={}:\[\],]*:\s*)[A-Za-z_$]\w*\b|(?<=\bdef\s+[A-Za-z_$]\w*\s*\([\w\s={}:\[\],]*\)\s*-\s*>\s*)[A-Za-z_$]\w*(?=\s*:)
        // This regex will fail if the parameters have a very complex default value

        ArrayList<Pattern> patterns = new ArrayList<>();
        patterns.add(commentPattern);
        patterns.add(stringPattern);
        patterns.add(keywordPattern);
        patterns.add(numberPattern);
        patterns.add(punctuationPattern);
        patterns.add(classPattern);
        patterns.add(functionPattern);
        
        ArrayList<CodeUnit> codeType = new ArrayList<>();
        codeType.add(CodeUnit.COMMENT);
        codeType.add(CodeUnit.STRING);
        codeType.add(CodeUnit.KEYWORD);
        codeType.add(CodeUnit.NUMBER);
        codeType.add(CodeUnit.PUNCTUATION);
        codeType.add(CodeUnit.CLASS);
        codeType.add(CodeUnit.FUNCTION);

        ArrayList<String> tokens = new ArrayList<>();
        ArrayList<ArrayList<CodeUnit>> tokenTypes = new ArrayList<>();
        codeTokenlize(code, patterns, codeType, tokens, tokenTypes);

        MutableComponent result = Component.empty();
        int tokenCount = tokens.size();
        for (int i = 0; i < tokenCount; i++) {
            String token = tokens.get(i);
            ArrayList<CodeUnit> type = tokenTypes.get(i);
            if (type.size() == 0) {
                result.append(Component.literal(token).withStyle(ChatFormatting.GRAY));
            } else if (type.contains(CodeUnit.STRING)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.GOLD));
            } else if (type.contains(CodeUnit.COMMENT)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.DARK_GREEN));
            } else if (type.contains(CodeUnit.KEYWORD)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            } else if (type.contains(CodeUnit.NUMBER)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (type.contains(CodeUnit.PUNCTUATION)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.DARK_AQUA));
            } else if (type.contains(CodeUnit.CLASS)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.GREEN));
            } else if (type.contains(CodeUnit.FUNCTION)) {
                result.append(Component.literal(token).withStyle(ChatFormatting.YELLOW));
            }
        }

        return result;
    }

    private static enum MarkdownUnit {
        CODE, MATH, HEADER, BOLD, ITALIC, STRIKE, INLINE, ENUM, HYPERLINK
    }

    private static enum CodeUnit {
        COMMENT, STRING, KEYWORD, PREPROCESSOR, NUMBER, PUNCTUATION, HEADER, CLASS, FUNCTION
    }
}