import React from 'react';
import { Link } from 'react-router-dom';
import { useAuthContext } from '../store/AuthContext';

export default function HomePage() {
  const { isAuthenticated } = useAuthContext();
  const logoSrc = process.env.PUBLIC_URL + '/logo512.png';

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="flex justify-center gap-4 ">
        <img src={logoSrc} alt="VectorDocs Logo" className="h-36 w-36"/>
      </div>

      <h1 className="text-3xl font-bold mb-6 flex flex-col-reverse md:flex-row md:items-center justify-between">
        Vector Docs
        <a href="https://github.com/vadim-miheev/vector-docs" target="_blank" className="mb-2">
          <img src="https://img.shields.io/badge/GitHub-Repository-blue?logo=github" alt="GitHub Repository"/>
        </a>
      </h1>

      <p className="text-gray-700 leading-relaxed mb-4">
        Vector Docs — is a <a href="https://en.wikipedia.org/wiki/Retrieval-augmented_generation" target="_blank"
                              className={'underline'}>RAG</a> service
        for uploading documents and intelligently searching their contents.
        The system builds vector representations of text and allows you to ask questions in natural language,
        receiving accurate answers with source information.
      </p>

      <h2 className="text-2xl font-semibold mt-8 mb-3">Purpose</h2>
      <ul className="list-disc pl-6 space-y-2 text-gray-800">
        <li>Storing and organizing personal documents.</li>
        <li>Quickly search by content instead of just matching words.</li>
        <li>Receiving responses with links to fragments of source files.</li>
      </ul>

      <h2 className="text-2xl font-semibold mt-8 mb-3">What is unique?</h2>
      <ul className="list-disc pl-6 space-y-2 text-gray-800">
        <li>Open source</li>
        <li>Can be used with any <a href={'https://en.wikipedia.org/wiki/Large_language_model'} target={'_blank'}
                                    className={'underline'}>LLM</a></li>
        <li>Shows good results with <a href={'https://ollama.com/library/qwen3:30b'} target={'_blank'}
                                       className={'underline'}>30b model</a> that can be run on your own hardware (AMD
          Radeon RX 7900 24GB or better)
        </li>
      </ul>

      <h2 className="text-2xl font-semibold mt-8 mb-3">Examples of use</h2>
      <ol className="list-decimal pl-6 space-y-3 text-gray-800">
        <li>
          Upload the PDF or TXT on the Documents page. The service will automatically index them
          for later search.
        </li>
        <li>
          Go to “Search” and ask a question in natural language: for example, “How do I set up a database connection?”
          or “What are the requirements for deployment?”
        </li>
        <li>
          Get an answer and a list of sources with the ability to open relevant document fragments.
        </li>
      </ol>

      <div className="mt-10 flex flex-wrap gap-3">
        {isAuthenticated ? (
          <>
            <Link to="/documents" className="px-4 py-2 rounded bg-gray-100 hover:bg-gray-200">My documents</Link>
            <Link to="/search" className="px-4 py-2 rounded bg-gray-100 hover:bg-gray-200">Go to search</Link>
          </>
        ) : (
          <>
            <Link to="/login" className="px-4 py-2 rounded bg-gray-100 hover:bg-gray-200">Login</Link>
            <Link to="/register" className="px-4 py-2 rounded bg-gray-100 hover:bg-gray-200">Register</Link>
          </>
        )}
      </div>

      <div className="mt-12 text-sm text-gray-500">
        This service is under development and may contain errors and inaccuracies. <a
        href="https://github.com/vadim-miheev/vector-docs/issues" className="text-blue-600 hover:underline">View the
        list of known issues</a>
      </div>
    </div>
  );
}
