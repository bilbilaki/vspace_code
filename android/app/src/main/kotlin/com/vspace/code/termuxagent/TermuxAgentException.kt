package com.vspace.code.termuxagent

class TermuxAgentException(
    val code: AgentErrorCode,
    override val message: String,
    val details: Any? = null,
) : Exception(message)
