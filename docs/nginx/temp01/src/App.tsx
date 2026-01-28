import { useState } from 'react'
import { ChatProvider } from './store/chatStore'
import { Sidebar } from './components/Layout/Sidebar'
import { ChatWindow } from './components/Chat/ChatWindow'
import { Composer } from './components/Input/Composer'
import { Menu } from 'lucide-react'

function App() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  return (
    <ChatProvider>
      <div className="flex h-screen w-full bg-white text-gray-900 overflow-hidden font-sans">
        <Sidebar isOpen={isSidebarOpen} setIsOpen={setIsSidebarOpen} />

        <div className="flex-1 flex flex-col h-full relative">
          {/* Mobile Header */}
          <div className="md:hidden flex items-center p-4 border-b border-gray-200">
            <button onClick={() => setIsSidebarOpen(!isSidebarOpen)} className="p-2 mr-2">
              <Menu size={20} />
            </button>
            <span className="font-bold">AutoAgent</span>
          </div>

          <ChatWindow />
          <Composer />
        </div>

        {/* Mobile Overlay */}
        {isSidebarOpen && (
          <div
            className="fixed inset-0 bg-black/50 z-40 md:hidden"
            onClick={() => setIsSidebarOpen(false)}
          />
        )}
      </div>
    </ChatProvider>
  )
}

export default App
