import { useState } from 'react';
import DatabaseManager from './DatabaseManager';
import LogSearch from './LogSearch'; // 1. THÊM IMPORT NÀY
import { Layers, Database } from 'lucide-react';

function App() {
  // Đổi mặc định tab đầu tiên mở ra là 'search' cho trực quan luôn nha Hải
  const [activeTab, setActiveTab] = useState<'search' | 'database'>('search');

  return (
    <div className="min-h-screen bg-gray-100 text-gray-900 selection:bg-indigo-500 selection:text-white">
      {/* Thanh Header Điều hướng */}
      <nav className="bg-white border-b border-gray-200 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex">
              <div className="flex-shrink-0 flex items-center mr-8">
                <span className="text-xl font-bold bg-linear-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
                  VDT AuditLog Admin
                </span>
              </div>
              
              <div className="flex space-x-4 items-center">
                <button
                  onClick={() => setActiveTab('search')}
                  className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition cursor-pointer ${
                    activeTab === 'search'
                      ? 'bg-indigo-50 text-indigo-700 font-semibold'
                      : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  <Layers className="w-4 h-4" />
                  Tra cứu Logs
                </button>

                <button
                  onClick={() => setActiveTab('database')}
                  className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition cursor-pointer ${
                    activeTab === 'database'
                      ? 'bg-indigo-50 text-indigo-700 font-semibold'
                      : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  <Database className="w-4 h-4" />
                  Quản lý DB Elastic
                </button>
              </div>
            </div>
          </div>
        </div>
      </nav>

      {/* Thay đổi vùng hiển thị nội dung theo Tab */}
      <main>
        {activeTab === 'database' ? (
          <DatabaseManager />
        ) : (
          <LogSearch /> // 2. THAY ĐOẠN KHUNG XIN LỖI CŨ THÀNH COMPONENT NÀY
        )}
      </main>
    </div>
  );
}

export default App;