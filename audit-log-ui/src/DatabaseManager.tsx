import React, { useState, useEffect } from 'react';
import { HardDrive, RefreshCw, Layers, CheckCircle2, AlertTriangle, HelpCircle } from 'lucide-react';

interface EsIndexInfo {
  indexName: string;
  health: string;  // green, yellow, red
  status: string;  // open, close
  docCount: string;
  storeSize: string;
}

export default function DatabaseManager() {
  const [indices, setIndices] = useState<EsIndexInfo[]>([]);
  const [loading, setLoading] = useState(false);

  // Hàm gọi API thật xuống Spring Boot để lấy data từ Elasticsearch
  const fetchIndices = async () => {
    setLoading(true);
    try {
      const res = await fetch('http://localhost:8080/api/v1/logs/indices');
      const data = await res.json();
      setIndices(data);
    } catch (error) {
      console.error("Lỗi khi fetch dữ liệu Elasticsearch index:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchIndices();
  }, []);

  // Hàm render màu sắc trạng thái Health của Node Index
  const getHealthBadge = (health: string) => {
    switch (health?.toLowerCase()) {
      case 'green':
        return <span className="inline-flex items-center gap-1 bg-green-50 text-green-700 border border-green-200 px-2 py-0.5 rounded-full text-xs font-semibold"><span className="w-1.5 h-1.5 rounded-full bg-green-500"></span>Green</span>;
      case 'yellow':
        return <span className="inline-flex items-center gap-1 bg-amber-50 text-amber-700 border border-amber-200 px-2 py-0.5 rounded-full text-xs font-semibold"><span className="w-1.5 h-1.5 rounded-full bg-amber-500"></span>Yellow</span>;
      case 'red':
        return <span className="inline-flex items-center gap-1 bg-red-50 text-red-700 border border-red-200 px-2 py-0.5 rounded-full text-xs font-semibold"><span className="w-1.5 h-1.5 rounded-full bg-red-500"></span>Red</span>;
      default:
        return <span className="inline-flex items-center gap-1 bg-gray-100 text-gray-600 border border-gray-200 px-2 py-0.5 rounded-full text-xs font-semibold"><span className="w-1.5 h-1.5 rounded-full bg-gray-400"></span>Unknown</span>;
    }
  };

  return (
    <div className="p-6 bg-gray-50 min-h-screen font-sans">
      <div className="max-w-7xl mx-auto">
        
        {/* Tiêu đề thanh công cụ */}
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center space-x-3">
            <Layers className="w-8 h-8 text-indigo-600" />
            <div>
              <h1 className="text-2xl font-bold text-gray-800">Quản trị Cluster Index Elasticsearch</h1>
              <p className="text-sm text-gray-500">Giám sát các phân vùng bộ nhớ và dung lượng lưu trữ thực tế</p>
            </div>
          </div>
          
          <button
            onClick={fetchIndices}
            disabled={loading}
            className="flex items-center gap-1.5 px-4 py-2 border border-gray-300 rounded-lg bg-white text-sm font-medium hover:bg-gray-50 text-gray-700 shadow-2xs transition disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới bộ nhớ
          </button>
        </div>

        {/* Bảng hiển thị Index Real-time */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200">
                  <th className="p-4 text-xs font-bold text-gray-500 uppercase">Tên Index (Elasticsearch)</th>
                  <th className="p-4 text-xs font-bold text-gray-500 uppercase">Trạng thái Sức khỏe</th>
                  <th className="p-4 text-xs font-bold text-gray-500 uppercase">Trạng thái Khối</th>
                  <th className="p-4 text-xs font-bold text-gray-500 uppercase text-right">Số lượng tài liệu (Docs)</th>
                  <th className="p-4 text-xs font-bold text-gray-500 uppercase text-right">Dung lượng ổ đĩa</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {loading && indices.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="text-center p-8 text-gray-500 font-medium">Đang quét cluster Elasticsearch...</td>
                  </tr>
                ) : indices.length > 0 ? (
                  indices.map((ind) => (
                    <tr key={ind.indexName} className="hover:bg-gray-50/70 transition">
                      <td className="p-4 text-sm font-semibold text-slate-800 font-mono">
                        {ind.indexName}
                      </td>
                      <td className="p-4 text-sm whitespace-nowrap">
                        {getHealthBadge(ind.health)}
                      </td>
                      <td className="p-4 text-sm whitespace-nowrap">
                        <span className="inline-block px-2 py-0.5 text-xs font-medium text-slate-600 bg-slate-100 rounded border border-slate-200 uppercase">
                          {ind.status}
                        </span>
                      </td>
                      <td className="p-4 text-sm font-mono text-gray-700 text-right font-medium">
                        {parseInt(ind.docCount).toLocaleString('vi-VN')}
                      </td>
                      <td className="p-4 text-sm font-mono text-indigo-600 text-right font-semibold">
                        {ind.storeSize}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5} className="text-center p-8 text-gray-400">Không tìm thấy index nào hoặc Elasticsearch đang trống dữ liệu.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </div>
  );
}