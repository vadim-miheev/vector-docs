import React, { useEffect, useRef, useState } from 'react';
import { useWebSocket } from '../hooks/useWebSocket';
import {apiClient, ENDPOINTS} from "../services/apiClient";
import {useNotifications} from "../store/NotificationsContext";
import {useAuth} from '../hooks/useAuth';
import DownloadLink from "../components/DownloadLink";
import { useParams, useNavigate } from 'react-router-dom';
import { fetchDemoJson, isDemoUser } from '../services/demoService';
import {useAuthContext} from "../store/AuthContext";

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
  const {user} = useAuthContext();
  const userMessagesKey = 'vd_user_' + user.id + '_messages'
  const [messages, setMessages] = useState(() => {
    try {
      const raw = localStorage.getItem(userMessagesKey);
      const parsed = raw ? JSON.parse(raw) : [];
      return Array.isArray(parsed) ? parsed : [];
    } catch (_) {
      return [];
    }
  });
  const [input, setInput] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [isRendering, setIsRendering] = useState(false);
  const currentRequestIDRef = useRef('');
  const currentAgentMessageRef = useRef({});
  const {addMessage} = useNotifications();
  const textareaRef = useRef(null);
  const messagesEndRef = useRef(null);
  const formRef = useRef(null);
  const conn = useWebSocket();
  const {logout} = useAuth();
  const { documentId } = useParams();
  const navigate = useNavigate();
  const [selectedDocId, setSelectedDocId] = useState('');
  const [selectedDocName, setSelectedDocName] = useState('');
  const showDemoHint = isDemoUser(user);
  const [searchExamples, setSearchExamples] = useState([]);

  // Last message tracking (React strict mode double calls fix)
  useEffect(() => {
    if (messages[messages.length - 1] && messages[messages.length - 1].role === 'agent') {
      currentAgentMessageRef.current = messages[messages.length - 1] ?? {};
    }
  }, [messages]);

  // Load document info from path param
  useEffect(() => {
    let cancelled = false;
    if (documentId) {
      setSelectedDocId(documentId);
      apiClient.get(`${ENDPOINTS.documents}/${encodeURIComponent(documentId)}`)
        .then((data) => {
          if (!cancelled) setSelectedDocName(data?.name || '');
        })
        .catch(() => {
          if (!cancelled) setSelectedDocName('');
        });
    } else {
      setSelectedDocId('');
      setSelectedDocName('');
    }
    return () => { cancelled = true; };
  }, [documentId]);

  // Load demo search examples if demo user is enabled
  useEffect(() => {
    let active = true;
    if (showDemoHint) {
      fetchDemoJson()
        .then((data) => {
          if (!active) return;
          const list = Array.isArray(data?.searchExamples) ? data.searchExamples : [];
          setSearchExamples(list);
        })
        .catch(() => {
          if (active) setSearchExamples([]);
        });
    } else {
      setSearchExamples([]);
    }
    return () => {
      active = false;
    };
  }, [showDemoHint]);

  // Persist messages to localStorage
  useEffect(() => {
    try {
      if (messages?.length) localStorage.setItem(userMessagesKey, JSON.stringify(messages));
      else localStorage.removeItem(userMessagesKey);
    } catch (_) {
      // ignore
    }
  }, [messages]);

  // Rendering messages from server (stream)
  useEffect(() => {
    function onChatResponse(e) {
      const payload = e?.detail ?? e;
      const currentId = currentRequestIDRef.current;

      if (payload?.requestId !== currentId) return;

      setIsSending(false);
      setIsRendering(true);

      if (payload?.complete === true) {
        currentAgentMessageRef.current = {};
        currentRequestIDRef.current = '';
        setIsRendering(false);
        return;
      }

      setMessages(prev => {
        const existing = prev.find(m => m.id === currentId);

        if (!existing) {
          currentAgentMessageRef.current = {
            id: currentId,
            role: 'agent',
            text: payload?.token ?? '',
            sources: payload?.sources ?? [],
          };
          return [...prev, currentAgentMessageRef.current];
        } else {
          const updated = prev.map(m => {
            if (m.id !== currentId) return m;
            return {
              ...m,
              text: (m.text ?? '') + (payload?.token ?? ''),
              sources: payload?.sources ?? m.sources,
            };
          });
          currentAgentMessageRef.current = updated.find(m => m.id === currentId);
          return updated;
        }
      });
    }

    window.addEventListener('chat:response', onChatResponse);
    return () => window.removeEventListener('chat:response', onChatResponse);
  }, []);

  // User message submission
  function sendMessage(e) {
    e?.preventDefault?.();

    if (isSending) return;
    if (conn.state() !== WebSocket.OPEN) {
      logout()
      return;
    }

    const trimmed = input.trim();
    if (!trimmed) return;

    // Add user message locally
    const userMsg = { id: `${Date.now()}-u`, role: 'user', text: trimmed };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');

    const history = messages.slice(-8).map((m) => ({ role: m.role, message: m.text }));

    setIsSending(true);
    currentRequestIDRef.current = `${Date.now()}-a`;
    const payload = {
      requestId: currentRequestIDRef.current,
      query: trimmed,
      context: history,
    };
    if (selectedDocId) {
      payload.documentId = selectedDocId;
    }
    apiClient.post(ENDPOINTS.search, payload).then(r => {
      if (r.status !== 202) {
        addMessage("Error: " + r.status + " " + r.statusText)
        setIsSending(false)
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

  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isSending]);

  return (
    <div className="text-black flex flex-col">
      {/* Header */}
      <header className="border-b border-gray-200 px-4 py-3 flex items-start md:items-center justify-between sticky top-[57px] z-10 bg-white">
        <div className="flex items-baseline gap-4 min-w-0 flex-col md:flex-row">
          <h1 className="text-lg font-semibold tracking-tight">AI Search</h1>
          {selectedDocId && (
            <span className="inline-flex items-center gap-2 text-xs border border-gray-300 rounded-md px-2 py-0.5 bg-gray-100 max-w-[80vw] md:max-w-xs">
              <span className="truncate" title={selectedDocName || selectedDocId}>{selectedDocName || 'Document'}</span>
              <button
                type="button"
                onClick={() => navigate('/search')}
                className="text-gray-500 hover:text-black"
                aria-label="Clear document filter"
                title="Clear document filter"
              >
                ×
              </button>
            </span>
          )}
        </div>
        <button
          type="button"
          onClick={clearChat}
          className="inline-flex items-center gap-2 rounded-md border border-gray-400 px-3 py-1.5 text-sm
          hover:bg-red-500 hover:text-white"
          title="Clear chat"
        >
          <span className="hidden sm:inline">Clear</span>
          <TrashIcon />
        </button>
      </header>

      {/* Messages area */}
      <main className="flex-1 overflow-y-auto px-4 py-6 mb-20 min-h-[50vh]" onClick={
        () => {
          if (messages?.length > 0) return
          // Input field ping
          textareaRef.current?.focus()
          formRef.current?.classList.add('animate-ping-custom')
          textareaRef.current?.classList.add('focus:bg-green-100')
          setTimeout(() => {
            formRef.current?.classList.remove('animate-ping-custom')
            textareaRef.current?.classList.remove('focus:bg-green-100')
          }, 1000)
        }
      }>
        {messages?.length === 0 ? (
          <div>
            {!showDemoHint && (<div className="text-neutral-400 text-sm">Ask a question below, the answer will appear here.</div>)}
            {showDemoHint && Array.isArray(searchExamples) && searchExamples.length > 0 && (
              <>
                <div className="text-neutral-500 text-sm mb-4">Ask your question below or try these example queries:</div>
                <div className="flex flex-wrap gap-2">
                  {searchExamples.map((ex, idx) => (
                    <button
                      key={idx}
                      type="button"
                      className="border border-blue-300 bg-blue-50 text-blue-800 hover:bg-blue-100 rounded-full px-2.5 py-1.5 text-xs"
                      onClick={() => {
                        setInput(ex);
                        if (textareaRef.current) {
                          textareaRef.current.focus();
                          autoResize();
                        }
                      }}
                      title="Click to copy into the search field"
                    >
                      {ex}
                    </button>
                  ))}
                </div>
              </>
            )}
          </div>
        ) : (
          <div className="space-y-6">
            {messages?.map((m) => (
              <div key={m.id} className={m.role === 'agent' ? '' : 'text-right'}>
                <div
                  className={
                    m.role === 'agent'
                      ? 'inline-block max-w-3xl rounded-lg bg-neutral-200 border border-gray-400 px-4 py-3 text-black relative'
                      : 'inline-block max-w-3xl rounded-lg border border-gray-400 px-4 py-3 text-black text-left'
                  }
                >
                  <div className="whitespace-pre-wrap leading-relaxed">{m.text.trim()}</div>
                  {m.role === 'agent' && Array.isArray(m.sources) && m.sources.length > 0 && (
                    <div className="pt-2">
                      <div className="text-xs text-neutral-500 mb-1">Sources:</div>
                      <ul className="space-y-1">
                        {m.sources.map((s, idx) => {
                          const title = s?.name || `Source ${idx + 1}`;
                          return (
                            <li key={idx} className={"flex items-start gap-2 hover:underline"}>
                              <span className="mt-2 h-1.5 w-1.5 rounded-full bg-black flex-shrink-0" />
                              <DownloadLink fileId={s?.id} fileName={title} page={s?.page} open={true} classes={"text-blue-900 hover:underline text-left"} onClick={() => {}}>
                                {title} (page {s?.page})
                              </DownloadLink>
                            </li>
                          );
                        })}
                      </ul>
                    </div>
                  )}
                  {isRendering && currentAgentMessageRef.current === m && (
                    <div className="absolute left-0 -bottom-6">
                      <svg className="animate-spin h-4 w-4 text-gray-500" xmlns="http://www.w3.org/2000/svg" fill="none"
                           viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                                strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor"
                              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                    </div>
                  )}
                </div>

              </div>
            ))}
            {isSending && (
              <div className="text-xs text-neutral-500">Waiting for response…</div>
            )}
            <div ref={messagesEndRef} />
          </div>
        )}
      </main>

      {/* Input bar */}
      <div className={`container fixed bottom-0 w-full backdrop-blur border-t border-gray-700 animate-slide-up`}
           style={{animationDelay: '500ms',animationFillMode: 'both'}}
      >
        <form onSubmit={sendMessage} className="relative w-full bottom-0 px-4 py-3" ref={formRef}>
          <div className="mx-auto w-full flex items-center gap-2">
            <textarea
              ref={textareaRef}
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
              className="flex-1 rounded-md border border-gray-600 px-3 py-2 text-sm placeholder-neutral-700
              focus:outline-none focus:ring-1 focus:ring-gray-400 focus:bg-white"
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  sendMessage();
                }
              }}
            />

            <button
              type="submit"
              disabled={!input.trim()}
              className="inline-flex items-center justify-center rounded-3xl border border-gray-600 px-2 py-2 text-sm
              hover:bg-blue-500 hover:text-white disabled:opacity-50 disabled:cursor-not-allowed"
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
