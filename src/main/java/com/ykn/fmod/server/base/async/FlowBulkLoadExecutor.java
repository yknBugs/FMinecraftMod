/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.async;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.flow.logic.LogicFlow;
import com.ykn.fmod.server.flow.tool.FlowManager;
import com.ykn.fmod.server.flow.tool.FlowSerializer;

/**
 * Reads and JSON-parses a batch of {@code .flow} files off the server thread, then merges the
 * results into {@link ServerData#getLogicFlows()} and reports outcomes via {@link ResultHandler}
 * on the server thread during the next tick.
 *
 * <p>Used by both {@code /f flow load *} and the startup auto-load
 * ({@link com.ykn.fmod.server.base.event.NewLevel}), which previously read and deserialized every
 * {@code .flow} file in the config directory synchronously on the server thread - noticeably
 * laggy for a large number of flows or large flow files.</p>
 *
 * @see AsyncTaskExecutor
 */
public class FlowBulkLoadExecutor extends AsyncTaskExecutor {

    /**
     * Callbacks for per-file and overall outcomes, invoked from {@link #taskAfterCompletion()}
     * on the server thread. Lets {@code /f flow load *} (chat feedback) and the startup
     * auto-load (console log / queued join message) share the same off-thread file loading
     * while keeping their own, differing feedback behavior.
     */
    public interface ResultHandler {

        /** 
         * The resolved path escaped the flow folder (e.g. a {@code ..} in the file name). 
         */
        void onFileNotFound(String fileName);

        /** 
         * {@link FlowSerializer#loadFile(Path)} returned {@code null} for this file. 
         */
        void onLoadFailed(String fileName);

        /** 
         * A flow with this name is already loaded; the file on disk was skipped. 
         */
        void onAlreadyExists(String flowName);

        /** 
         * All files have been processed; {@code loadedCount} were newly loaded. 
         */
        void onCompleted(int loadedCount);
    }

    private final List<String> fileNames;
    private final Path flowFolder;
    private final ServerData data;
    private final ResultHandler resultHandler;

    private final List<LogicFlow> parsedFlows = new ArrayList<>();
    private final List<String> notFoundFiles = new ArrayList<>();
    private final List<String> failedFiles = new ArrayList<>();

    /**
     * @param fileNames    the {@code .flow} file names (relative to {@code flowFolder}) to load;
     *                     copied defensively by the caller before this is submitted so it's safe
     *                     to iterate from the background thread
     * @param flowFolder   the config directory that flow files must resolve within
     * @param data         the {@link ServerData} to merge newly loaded flows into
     * @param resultHandler callbacks for per-file and overall outcomes
     */
    public FlowBulkLoadExecutor(List<String> fileNames, Path flowFolder, ServerData data, ResultHandler resultHandler) {
        this.fileNames = fileNames;
        this.flowFolder = flowFolder;
        this.data = data;
        this.resultHandler = resultHandler;
    }

    @Override
    protected void executeAsyncTask() {
        for (String flowFileName : fileNames) {
            Path flowPath = flowFolder.resolve(flowFileName).normalize();
            if (!flowPath.startsWith(flowFolder)) {
                notFoundFiles.add(flowFileName);
                continue;
            }
            LogicFlow flow = FlowSerializer.loadFile(flowPath);
            if (flow == null) {
                failedFiles.add(flowFileName);
                continue;
            }
            parsedFlows.add(flow);
        }
    }

    @Override
    protected void taskAfterCompletion() {
        notFoundFiles.forEach(resultHandler::onFileNotFound);
        failedFiles.forEach(resultHandler::onLoadFailed);
        int loadedCount = 0;
        for (LogicFlow flow : parsedFlows) {
            if (data.getLogicFlows().get(flow.getName()) != null) {
                resultHandler.onAlreadyExists(flow.getName());
                continue;
            }
            FlowManager flowManager = new FlowManager(flow);
            data.getLogicFlows().put(flow.getName(), flowManager);
            flowManager.setEnabled(true);
            loadedCount++;
        }
        resultHandler.onCompleted(loadedCount);
    }
}
