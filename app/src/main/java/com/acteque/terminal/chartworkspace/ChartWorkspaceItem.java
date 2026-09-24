package com.acteque.terminal.chartworkspace;

/** A node in the recursively split chart-workspace tree. */
sealed interface ChartWorkspaceItem permits ChartWorkspaceLeaf, ChartWorkspaceSplit {}
