package org.jenkinsci.plugins.unity3d.logs.block;

import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: clement.dagneau
 * Date: 16/12/2011
 * Time: 09:52
 */
public class UnityBlockList {
    public static final List<Block> editorLogBlocks = List.of(
            new PlayerStatisticsBlock(),
            new CompileBlock(),
            new PrepareBlock(),
            new LightmapBlock(),
            new UpdateBlock()
    );
}
