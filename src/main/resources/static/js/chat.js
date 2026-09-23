const state = {
    conversations: [],
    currentConversationId: null,
    sending: false
};

const conversationList = document.querySelector('#conversation-list');
const conversationTitle = document.querySelector('#conversation-title');
const messageList = document.querySelector('#message-list');
const newConversationButton = document.querySelector('#new-conversation-button');
const chatForm = document.querySelector('#chat-form');
const messageInput = document.querySelector('#message-input');
const sendButton = document.querySelector('#send-button');
const chatError = document.querySelector('#chat-error');
const sidebarToggle = document.querySelector('#sidebar-toggle');
const sidebarClose = document.querySelector('#sidebar-close');
const sidebarBackdrop = document.querySelector('#sidebar-backdrop');
const coarsePointer = window.matchMedia('(pointer: coarse)');

document.addEventListener('DOMContentLoaded', initializePage);
newConversationButton.addEventListener('click', createConversation);
chatForm.addEventListener('submit', sendMessage);
messageInput.addEventListener('keydown', handleInputKeydown);
messageInput.addEventListener('input', autoResizeInput);
messageInput.addEventListener('focus', keepComposerVisible);
sidebarToggle.addEventListener('click', () => setSidebarOpen(true));
sidebarClose.addEventListener('click', () => setSidebarOpen(false));
sidebarBackdrop.addEventListener('click', () => setSidebarOpen(false));
document.addEventListener('keydown', event => {
    if (event.key === 'Escape') {
        setSidebarOpen(false);
    }
});
window.addEventListener('resize', () => {
    if (window.innerWidth > 700) {
        setSidebarOpen(false);
    }
});

const viewport = window.visualViewport;
if (viewport) {
    viewport.addEventListener('resize', updateViewportHeight);
    viewport.addEventListener('scroll', updateViewportHeight);
}
updateViewportHeight();

async function initializePage() {
    try {
        await refreshConversationList();
        if (state.conversations.length > 0) {
            await openConversation(state.conversations[0].id);
        } else {
            showWelcomeState();
        }
    } catch (error) {
        showError(error.message);
    }
}

async function apiRequest(url, options = {}) {
    const headers = { ...(options.headers || {}) };
    if (options.body) {
        headers['Content-Type'] = 'application/json';
    }

    const response = await fetch(url, {
        ...options,
        headers,
        credentials: 'same-origin'
    });

    if (response.status === 401) {
        window.location.replace('/');
        throw new Error('登录状态已失效，请重新登录');
    }

    if (!response.ok) {
        let message = '请求失败，请稍后重试';
        try {
            const error = await response.json();
            message = error.message || message;
        } catch (ignored) {
            // 后端没有返回 JSON 时使用默认提示。
        }
        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }
    return response.json();
}

async function refreshConversationList() {
    state.conversations = await apiRequest('/api/conversations');
    renderConversationList();
}

function renderConversationList() {
    conversationList.replaceChildren();

    if (state.conversations.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'conversation-empty';
        empty.textContent = '还没有历史会话';
        conversationList.appendChild(empty);
        return;
    }

    for (const conversation of state.conversations) {
        const item = document.createElement('div');
        item.className = 'conversation-item';
        if (conversation.id === state.currentConversationId) {
            item.classList.add('active');
        }

        const selectButton = document.createElement('button');
        selectButton.type = 'button';
        selectButton.className = 'conversation-select';
        selectButton.textContent = conversation.title;
        selectButton.title = conversation.title;
        selectButton.addEventListener('click', () => openConversation(conversation.id));

        const deleteButton = document.createElement('button');
        deleteButton.type = 'button';
        deleteButton.className = 'conversation-delete';
        deleteButton.textContent = '×';
        deleteButton.title = '删除会话';
        deleteButton.setAttribute('aria-label', `删除会话：${conversation.title}`);
        deleteButton.addEventListener('click', () => deleteConversation(conversation.id));

        item.append(selectButton, deleteButton);
        conversationList.appendChild(item);
    }
}

async function createConversation() {
    clearError();
    newConversationButton.disabled = true;
    try {
        const conversation = await apiRequest('/api/conversations', { method: 'POST' });
        await refreshConversationList();
        await openConversation(conversation.id);
        setSidebarOpen(false);
        messageInput.focus();
    } catch (error) {
        showError(error.message);
    } finally {
        newConversationButton.disabled = false;
    }
}

async function openConversation(conversationId) {
    clearError();
    setSidebarOpen(false);
    state.currentConversationId = conversationId;
    renderConversationList();

    const conversation = state.conversations.find(item => item.id === conversationId);
    conversationTitle.textContent = conversation ? conversation.title : 'AI 助手';
    setComposerEnabled(true);
    messageList.replaceChildren(createLoadingMessage('正在加载消息...'));

    try {
        const messages = await apiRequest(`/api/conversations/${conversationId}/messages`);
        if (state.currentConversationId !== conversationId) {
            return;
        }
        renderMessages(messages);
    } catch (error) {
        if (state.currentConversationId === conversationId) {
            showError(error.message);
            messageList.replaceChildren();
        }
    }
}

async function deleteConversation(conversationId) {
    const conversation = state.conversations.find(item => item.id === conversationId);
    const title = conversation ? conversation.title : '这个会话';
    if (!window.confirm(`确定删除“${title}”吗？聊天记录也会一起删除。`)) {
        return;
    }

    clearError();
    try {
        await apiRequest(`/api/conversations/${conversationId}`, { method: 'DELETE' });
        const deletedCurrent = state.currentConversationId === conversationId;
        if (deletedCurrent) {
            state.currentConversationId = null;
        }
        await refreshConversationList();

        if (deletedCurrent && state.conversations.length > 0) {
            await openConversation(state.conversations[0].id);
        } else if (state.conversations.length === 0) {
            showWelcomeState();
        }
    } catch (error) {
        showError(error.message);
    }
}

async function sendMessage(event) {
    event.preventDefault();
    const question = messageInput.value.trim();
    if (!question || state.sending || state.currentConversationId === null) {
        return;
    }

    const conversationId = state.currentConversationId;
    state.sending = true;
    clearError();
    setComposerEnabled(false);
    messageInput.value = '';
    autoResizeInput();

    if (messageList.querySelector('.empty-state')) {
        messageList.replaceChildren();
    }
    appendMessage('user', question);
    const typingMessage = appendMessage('assistant', '正在思考...', true);

    try {
        const response = await apiRequest('/api/chat', {
            method: 'POST',
            body: JSON.stringify({
                conversationId,
                message: question
            })
        });

        if (state.currentConversationId === conversationId) {
            typingMessage.remove();
            appendMessage('assistant', response.reply);
        }
        await refreshConversationList();

        const current = state.conversations.find(item => item.id === state.currentConversationId);
        if (current) {
            conversationTitle.textContent = current.title;
        }
    } catch (error) {
        typingMessage.remove();
        showError(error.message);
        if (state.currentConversationId === conversationId && !messageInput.value) {
            messageInput.value = question;
            autoResizeInput();
        }
    } finally {
        state.sending = false;
        setComposerEnabled(state.currentConversationId !== null);
        messageInput.focus();
    }
}

function renderMessages(messages) {
    messageList.replaceChildren();
    if (messages.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'empty-state';

        const icon = document.createElement('div');
        icon.className = 'empty-icon';
        icon.textContent = '✦';

        const heading = document.createElement('h2');
        heading.textContent = '开始一段新对话';

        const hint = document.createElement('p');
        hint.textContent = '例如：请简单介绍一下 Spring Boot。';

        empty.append(icon, heading, hint);
        messageList.appendChild(empty);
        return;
    }

    for (const message of messages) {
        appendMessage(message.role, message.content);
    }
    scrollToLatestMessage();
}

function appendMessage(role, content, typing = false) {
    const row = document.createElement('div');
    row.className = `message-row ${role}`;
    if (typing) {
        row.classList.add('typing');
    }

    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.textContent = role === 'user' ? '我' : 'AI';

    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';
    bubble.textContent = content;

    row.append(avatar, bubble);
    messageList.appendChild(row);
    scrollToLatestMessage();
    return row;
}

function createLoadingMessage(text) {
    const loading = document.createElement('div');
    loading.className = 'empty-state';
    const hint = document.createElement('p');
    hint.textContent = text;
    loading.appendChild(hint);
    return loading;
}

function showWelcomeState() {
    state.currentConversationId = null;
    conversationTitle.textContent = 'AI 助手';
    setComposerEnabled(false);
    renderConversationList();

    const empty = document.createElement('div');
    empty.className = 'empty-state';
    const icon = document.createElement('div');
    icon.className = 'empty-icon';
    icon.textContent = '✦';
    const heading = document.createElement('h2');
    heading.textContent = '你好，我是 AI 助手';
    const hint = document.createElement('p');
    hint.textContent = '点击“新建对话”，然后输入你想了解的问题。';
    empty.append(icon, heading, hint);
    messageList.replaceChildren(empty);
}

function setComposerEnabled(enabled) {
    messageInput.disabled = !enabled || state.sending;
    sendButton.disabled = !enabled || state.sending;
}

function handleInputKeydown(event) {
    if (event.key === 'Enter' && !event.shiftKey && !coarsePointer.matches) {
        event.preventDefault();
        chatForm.requestSubmit();
    }
}

function autoResizeInput() {
    messageInput.style.height = 'auto';
    messageInput.style.height = `${Math.min(messageInput.scrollHeight, 130)}px`;
}

function scrollToLatestMessage() {
    messageList.scrollTop = messageList.scrollHeight;
}

function setSidebarOpen(open) {
    document.body.classList.toggle('sidebar-open', open);
    sidebarToggle.setAttribute('aria-expanded', String(open));
    sidebarBackdrop.hidden = !open;
}

function updateViewportHeight() {
    const height = viewport ? viewport.height : window.innerHeight;
    document.documentElement.style.setProperty('--app-height', `${Math.round(height)}px`);
}

function keepComposerVisible() {
    window.requestAnimationFrame(() => {
        updateViewportHeight();
        scrollToLatestMessage();
    });
}

function showError(message) {
    chatError.textContent = message || '操作失败，请稍后重试';
}

function clearError() {
    chatError.textContent = '';
}
