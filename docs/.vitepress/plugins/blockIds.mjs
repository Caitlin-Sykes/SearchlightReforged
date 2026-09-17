export default function blockIds(md) {
    md.inline.ruler.before('text', 'block_ids', blockIdsRule)

    md.renderer.rules.block_ids = (tokens, idx) => {
        const token = tokens[idx]

        const ids = token.meta.ids

        return `<BlockIds ids="${escapeAttribute(JSON.stringify(ids))}"></BlockIds>`
    }
}

function blockIdsRule(state, silent) {
    const start = state.pos
    const src = state.src

    // Must start with "@["
    if (src[start] !== '@' || src[start + 1] !== '[') {
        return false
    }

    // Find the closing "]"
    const end = src.indexOf(']', start + 2)

    if (end === -1) {
        return false
    }

    const contents = src.slice(start + 2, end)

    // Don't consume an empty expression
    if (!contents.trim()) {
        return false
    }

    const ids = contents
        .split(',')
        .map(id => id.trim()
            .replace('"', '')
            .replace('[', '')
            .replace(']', ''))
        .filter(Boolean)

    if (ids.length === 0) {
        return false
    }

    // During the parser's "silent" pass we only report
    // whether this rule matched.
    if (silent) {
        return true
    }

    const token = state.push('block_ids', '', 0)

    token.content = contents
    token.meta = {
        ids
    }

    state.pos = end + 1

    return true
}

function escapeAttribute(value) {
    return value
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
}