package org.projectfluent.syntax.smart

import org.projectfluent.syntax.ast.*
import java.lang.Exception

class SmartPattern(vararg elements: PatternElement) : SyntaxNode() {
    val elements: MutableList<PatternElement> = mutableListOf()

    init {
        this.elements += elements
    }
}

fun toSmartPattern(pattern: Pattern): SmartPattern {
    val result = SmartPattern()
    for (elem in smartElements(pattern)) {
        result.elements += elem
    }
    return result
}

fun smartElements(pattern: Pattern) = sequence {
    var lastText: TextElement? = null
    pattern.elements.forEach { element ->
        when (element) {
            is TextElement -> {
                if (lastText == null) {
                    lastText = element
                } else {
                    lastText?.let { it.value += element.value }
                }
            }
            is Placeable -> {
                when (val expression = element.expression) {
                    is NumberLiteral -> {
                        val content = expression.value
                        if (lastText == null) {
                            lastText = TextElement("")
                        }
                        lastText?.let { it.value += content }
                    }
                    is StringLiteral -> {
                        var content = expression.value
                        content = special.replace(content) { m -> unescape(m) }
                        if (lastText == null) {
                            lastText = TextElement("")
                        }
                        lastText?.let { it.value += content }
                    }
                    is SelectExpression -> {
                        throw Exception("Bad Pattern content for AntiPattern")
                    }
                    else -> {
                        lastText?.let {
                            yield(it)
                            lastText = null
                        }
                        yield(element)
                    }
                }
            }
        }
    }
    lastText?.let { yield(it) }
}

val special =
    """\\(([\\"])|(u[0-9a-fA-F]{4}))""".toRegex()
private fun unescape(matchResult: MatchResult): CharSequence {
    val matches = matchResult.groupValues.drop(2).listIterator()
    val simple = matches.next()
    if (simple != "") { return simple }
    val uni4 = matches.next()
    if (uni4 != "") {
        val a = uni4.substring(1).toInt(16)
        val b = a.toChar()
        val c = b.toString()
        val d = a.toString()
        return uni4.substring(1).toInt(16).toChar().toString()
    }
    throw Exception("Unexpected")
}