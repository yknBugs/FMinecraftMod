package com.ykn.fmod.server.flow.tool;

import java.util.function.Consumer;

import com.ykn.fmod.server.flow.logic.LogicFlow;

public class NodeEditPath {

    private Consumer<LogicFlow> redoLogic;

    private Consumer<LogicFlow> undoLogic;

    public NodeEditPath(Consumer<LogicFlow> redo, Consumer<LogicFlow> undo) {
        this.redoLogic = redo;
        this.undoLogic = undo;
    }

    public void redo(LogicFlow flow) {
        this.redoLogic.accept(flow);
    }

    public void undo(LogicFlow flow) {
        this.undoLogic.accept(flow);
    }

}
