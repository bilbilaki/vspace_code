package com.vspace.code.termuxagent

enum class AgentErrorCode(val wireValue: String) {
    VALIDATION("validation"),
    IO("io"),
    PROCESS("process"),
    WORKSPACE("workspace"),
    BOOTSTRAP("bootstrap"),
    PERMISSION("permission"),
    UNKNOWN("unknown"),
}
