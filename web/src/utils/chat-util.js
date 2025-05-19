export const parseThinkingMessage = (text) => {
    if (!text) return { think: '', content: text || '' };

    let thinkContent = '';
    let content = text;

    // 检查是否包含完整的<think>标签
    const hasOpenTag = text.includes('<think>');
    const hasCloseTag = text.includes('</think>');

    if (hasOpenTag && hasCloseTag) {
        // 完整标签情况 - 提取<think>...</think>中的内容
        const thinkMatch = text.match(/<think>([\s\S]*?)<\/think>/);
        if (thinkMatch) {
            thinkContent = thinkMatch[1].trim();
            // 移除<think>...</think>标签和其中内容
            content = text.replace(/<think>[\s\S]*?<\/think>/g, '').trim();
        }
    } else if (hasOpenTag) {
        thinkContent = text.replace('<think>', '').trim();
        content = '';
    }

    return {
        think: thinkContent,
        content: content,
        raw: text
    };
};