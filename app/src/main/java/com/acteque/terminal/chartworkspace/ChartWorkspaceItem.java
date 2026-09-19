package com.acteque.terminal.chartworkspace;

sealed interface ChartWorkspaceItem permits ChartWorkspaceLeaf, ChartWorkspaceSplit {}
