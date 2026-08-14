/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.async;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.rule.core.CustomRule;
import com.ykn.fmod.server.rule.tool.RuleManager;
import com.ykn.fmod.server.rule.tool.RuleSerializer;

/**
 * Reads and JSON-parses a batch of {@code .rule} files off the server thread, then merges the
 * results into {@link ServerData#getCustomRules()} and reports outcomes via {@link ResultHandler}
 * on the server thread during the next tick.
 *
 * <p>Used by both {@code /f rule load *} and the startup auto-load
 * ({@link com.ykn.fmod.server.base.event.NewLevel}), which previously read and deserialized every
 * {@code .rule} file in the config directory synchronously on the server thread - noticeably
 * laggy for a large number of rules.</p>
 *
 * @see AsyncTaskExecutor
 */
public class RuleBulkLoadExecutor extends AsyncTaskExecutor {

    /**
     * Callbacks for per-file and overall outcomes, invoked from {@link #taskAfterCompletion()}
     * on the server thread. Lets {@code /f rule load *} (chat feedback) and the startup
     * auto-load (console log / queued join message) share the same off-thread file loading
     * while keeping their own, differing feedback behavior.
     */
    public interface ResultHandler {

        /** 
         * The resolved path escaped the rule folder (e.g. a {@code ..} in the file name). 
         */
        void onFileNotFound(String fileName);

        /** 
         * {@link RuleSerializer#loadFile(Path)} returned {@code null} for this file. 
         */
        void onLoadFailed(String fileName);

        /** 
         * A rule with this name is already loaded; the file on disk was skipped.
         */
        void onAlreadyExists(String ruleName);

        /** 
         * All files have been processed; {@code loadedCount} were newly loaded.
         */
        void onCompleted(int loadedCount);
    }

    private final List<String> fileNames;
    private final Path ruleFolder;
    private final ServerData data;
    private final ResultHandler resultHandler;

    private final List<CustomRule> parsedRules = new ArrayList<>();
    private final List<String> notFoundFiles = new ArrayList<>();
    private final List<String> failedFiles = new ArrayList<>();

    /**
     * @param fileNames    the {@code .rule} file names (relative to {@code ruleFolder}) to load;
     *                     copied defensively by the caller before this is submitted so it's safe
     *                     to iterate from the background thread
     * @param ruleFolder   the config directory that rule files must resolve within
     * @param data         the {@link ServerData} to merge newly loaded rules into
     * @param resultHandler callbacks for per-file and overall outcomes
     */
    public RuleBulkLoadExecutor(List<String> fileNames, Path ruleFolder, ServerData data, ResultHandler resultHandler) {
        this.fileNames = fileNames;
        this.ruleFolder = ruleFolder;
        this.data = data;
        this.resultHandler = resultHandler;
    }

    @Override
    protected void executeAsyncTask() {
        for (String ruleFileName : fileNames) {
            Path rulePath = ruleFolder.resolve(ruleFileName).normalize();
            if (!rulePath.startsWith(ruleFolder)) {
                notFoundFiles.add(ruleFileName);
                continue;
            }
            CustomRule rule = RuleSerializer.loadFile(rulePath);
            if (rule == null) {
                failedFiles.add(ruleFileName);
                continue;
            }
            parsedRules.add(rule);
        }
    }

    @Override
    protected void taskAfterCompletion() {
        notFoundFiles.forEach(resultHandler::onFileNotFound);
        failedFiles.forEach(resultHandler::onLoadFailed);
        int loadedCount = 0;
        for (CustomRule rule : parsedRules) {
            if (data.getCustomRules().get(rule.getName()) != null) {
                resultHandler.onAlreadyExists(rule.getName());
                continue;
            }
            RuleManager ruleManager = new RuleManager(rule);
            data.getCustomRules().put(rule.getName(), ruleManager);
            ruleManager.setEnabled(true);
            loadedCount++;
        }
        resultHandler.onCompleted(loadedCount);
    }
}
