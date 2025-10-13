import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useWebSocket } from '../hooks/useWebSocket';
import {apiClient, ENDPOINTS} from "../services/apiClient";
import {useNotifications} from "../store/NotificationsContext";

function SendIcon({ className = 'w-5 h-5' }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2.5"
         aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M5 15l7-7 7 7"/>
    </svg>
  );
}

function TrashIcon({ className = 'w-5 h-5' }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M6 7h12M9 7V5a2 2 0 012-2h2a2 2 0 012 2v2m-7 0v12a2 2 0 002 2h4a2 2 0 002-2V7" />
    </svg>
  );
}

export default function SearchPage() {
  const [messages, setMessages] = useState(() => {
    try {
      const raw = localStorage.getItem('vd_messages');
      const parsed = raw ? JSON.parse(raw) : [];
      return Array.isArray(parsed) ? parsed : [];
    } catch (_) {
      return [];
    }
  });
  const [input, setInput] = useState('');
  const [isSending, setIsSending] = useState(false);
  const lastRequestIDRef = useRef('');
  const lastMessageRef = useRef({});
  const {addMessage} = useNotifications();
  const textareaRef = useRef(null);

  // Last message tracking (React strict mode double calls fix)
  useEffect(() => {
    lastMessageRef.current = messages[messages.length - 1] ?? {};
  }, [messages]);

  // Persist messages to localStorage
  useEffect(() => {
    try {
      if (messages?.length) localStorage.setItem('vd_messages', JSON.stringify(messages));
      else localStorage.removeItem('vd_messages');
    } catch (_) {
      // ignore
    }
  }, [messages]);

  // Rendering messages from server
  useEffect(() => {
    function onChatResponse(e) {
      const payload = e?.detail ?? e;

      if (payload?.requestId === lastRequestIDRef.current) {
        if (payload?.complete === true) {
          setIsSending(false)
          return
        }

        if (lastMessageRef.current.id === lastRequestIDRef.current) {
          lastMessageRef.current.text += payload?.token;
          setMessages(prevState => {
            const updatedMessages = [...prevState];
            return updatedMessages.map(m => {
              if (m.id === lastRequestIDRef.current) {
                return lastMessageRef.current;
              }
              return m;
            })
          })
        }
      }
    }

    window.addEventListener('chat:response', onChatResponse);
    return () => window.removeEventListener('chat:response', onChatResponse);
  }, []);

  // User message submission
  function sendMessage(e) {
    e?.preventDefault?.();
    const trimmed = input.trim();
    if (!trimmed) return;

    // Add user message locally
    const userMsg = { id: `${Date.now()}-u`, role: 'user', text: trimmed };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');

    const history = messages.slice(-8).map((m) => ({ role: m.role, message: m.text }));

    setIsSending(true);
    lastRequestIDRef.current = `${Date.now()}-a`;
    apiClient.post(ENDPOINTS.search, {
      'requestId': lastRequestIDRef.current,
      'query': trimmed,
      'context': history
    }).then(r => {
      console.log("Response: ", r)
      if (r.status !== 202) {
        addMessage("Error: " + r.status + " " + r.statusText)
        setIsSending(false)
      } else {
        setMessages(prev => [
          ...prev,
          { id: lastRequestIDRef.current, role: 'assistant', text: '', sources: [] }
        ])
      }
    });
  }

  function clearChat() {
    setMessages([]);
  }

  function autoResize() {
    const el = textareaRef.current;
    if (el) {
      el.style.height = 'auto';
      const maxHeight = 160;
      el.style.height = Math.min(el.scrollHeight, maxHeight) + 'px';
      el.style.overflowY = el.scrollHeight > maxHeight ? 'auto' : 'hidden';
    }
  }

  useEffect(() => {
    autoResize();
  }, [input]);


  return (
    <div className="text-black flex flex-col">
      {/* Header */}
      <header className="border-b border-gray-200 px-4 py-3 flex items-center justify-between">
        <h1 className="text-lg font-semibold tracking-tight">AI Search</h1>
        <button
          type="button"
          onClick={clearChat}
          className="inline-flex items-center gap-2 rounded-md border border-gray-400 px-3 py-1.5 text-sm hover:bg-neutral-900 hover:text-white"
          title="Clear chat"
        >
          <span className="hidden sm:inline">Clear</span>
          <TrashIcon />
        </button>
      </header>

      {/* Messages area */}
      <main className="flex-1 overflow-y-auto px-4 py-6">
        {messages?.length === 0 ? (
          <div className="text-neutral-400 text-sm">Ask a question below, the answer will appear here.</div>
        ) : (
          <div className="space-y-6">
            {messages?.map((m) => (
              <div key={m.id} className={m.role === 'assistant' ? '' : 'text-right'}>
                <div
                  className={
                    m.role === 'assistant'
                      ? 'inline-block max-w-3xl rounded-lg bg-neutral-200 border border-gray-400 px-4 py-3 text-black'
                      : 'inline-block max-w-3xl rounded-lg border border-gray-400 px-4 py-3 text-black'
                  }
                >
                  <div className="whitespace-pre-wrap leading-relaxed">{m.text}</div>
                  {m.role === 'assistant' && Array.isArray(m.sources) && m.sources.length > 0 && (
                    <div className="mt-3 border-t border-neutral-800 pt-2">
                      <div className="text-xs text-neutral-400 mb-1">Sources:</div>
                      <ul className="list-disc list-inside space-y-1">
                        {m.sources.map((s, idx) => {
                          const title = s?.title || s?.name || s?.url || `Source ${idx + 1}`;
                          const href = s?.url || s?.link || '#';
                          return (
                            <li key={idx}>
                              <a
                                href={href}
                                target="_blank"
                                rel="noreferrer"
                                className="text-neutral-200 hover:underline"
                              >
                                {title}
                              </a>
                            </li>
                          );
                        })}
                      </ul>
                    </div>
                  )}
                </div>
              </div>
            ))}
            {isSending && (
              <div className="text-xs text-neutral-500">Waiting for response…</div>
            )}
          </div>
        )}
      </main>

      {/* Input bar */}
      <div className={"container fixed bottom-0 w-full"}>
        <form onSubmit={sendMessage} className="relative w-full bottom-0 border-t border-gray-300 backdrop-blur px-4 py-3">
          <div className="mx-auto w-full flex items-center gap-2">
          <textarea
            ref={el => (textareaRef.current = el)}
            value={input}
            onChange={(e) => {
              setInput(e.target.value);
              autoResize();
            }}
            onInput={autoResize}
            rows={1}
            style={{
              maxHeight: '160px',
              resize: 'none',
            }}
            placeholder={'Ask a question…'}
            className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm placeholder-neutral-500 focus:outline-none focus:ring-1 focus:ring-gray-400"
          />

            <button
              type="submit"
              disabled={!input.trim()}
              className="inline-flex items-center justify-center rounded-3xl border border-gray-400 px-2 py-2 text-sm hover:bg-black hover:text-white disabled:opacity-40"
              title="Send"
            >
              <SendIcon />
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
