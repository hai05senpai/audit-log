import React, { useState, useEffect } from 'react';
import { Search, Filter, Calendar, User, Activity, ChevronLeft, ChevronRight, Loader2 } from 'lucide-react';

interface AuditLogEvent {
  id?: string;
  timestamp: string;
  actor: string;
  actionType: string;
  description: string;
  queryStatement: string; // 1. Thêm trường queryStatement vào interface
  status: string;
  ipAddress?: string;
}

export default function LogSearch() {
  // States cho các điều kiện tìm kiếm
  const [query, setQuery] = useState('');
  const [actor, setActor] = useState('');
  const [actionType, setActionType] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  
  // States quản lý dữ liệu và phân trang
  const [logs, setLogs] = useState<AuditLogEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const size = 10; // Số lượng log trên mỗi trang

  // Hàm gọi API tìm kiếm xuống Spring Boot
  const handleSearch = async (targetPage = page) => {
    setLoading(true);
    try {
      // Xây dựng URL với các query parameters
      const params = new URLSearchParams({
        page: targetPage.toString(),
        size: size.toString(),
      });

      if (query) params.append('query', query);
      if (actor) params.append('actor', actor);
      if (actionType) params.append('actionType', actionType);
      if (startDate) {
        const fromMillis = new Date(`${startDate}T00:00:00`).getTime();
        params.append('fromTime', fromMillis.toString());
      }
      if (endDate) {
        const toMillis = new Date(`${endDate}T23:59:59`).getTime();
        params.append('toTime', toMillis.toString());
      }

      const res = await fetch(`http://localhost:8080/api/v1/logs/search?${params.toString()}`);
      const data = await res.json();

      // Đọc dữ liệu trả về từ đối tượng Page của Spring Data
      setLogs(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
      setPage(targetPage);
    } catch (error) {
      console.error("Lỗi khi tìm kiếm audit logs:", error);
    } finally {
      setLoading(false);
    }
  };

  // Tự động gọi tìm kiếm lần đầu khi component mount
  useEffect(() => {
    handleSearch(0);
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleSearch(0); // Tìm kiếm mới thì reset về trang 0
  };

  return (
    <div className="p-6 bg-gray-50 min-h-screen font-sans">
      <div className="max-w-7xl mx-auto">
        
        {/* Tiêu đề */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-800">Hệ thống Tra cứu Audit Logs</h1>
          <p className="text-sm text-gray-500">Tìm kiếm Full-text search nâng cao kết hợp đa điều kiện trên Elasticsearch</p>
        </div>

        {/* Form Bộ lọc tìm kiếm */}
        <form onSubmit={handleSubmit} className="bg-white p-5 rounded-xl shadow-xs border border-gray-200 mb-6 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            
            {/* Full-text query */}
            <div className="md:col-span-3 relative">
              <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Từ khóa truy vấn (Full-text trên Query Statement)</label>
              <div className="relative">
                <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="Nhập từ khóa trong câu lệnh SQL cần truy vết (e.g. SELECT, WHERE, tên_bảng...)"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 font-mono"
                />
              </div>
            </div>

            {/* Lọc theo Người tác động */}
            <div>
              <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Actor (Người thực hiện)</label>
              <div className="relative">
                <User className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="e.g. admin, user_01"
                  value={actor}
                  onChange={(e) => setActor(e.target.value)}
                  className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>

            {/* Lọc theo Loại hành động */}
            <div>
              <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Action Type (Hành động)</label>
              <div className="relative">
                <Activity className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="e.g. DML, DQL"
                  value={actionType}
                  onChange={(e) => setActionType(e.target.value)}
                  className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>

            {/* Khoảng thời gian */}
            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Từ ngày</label>
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Đến ngày</label>
                <input
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>

          </div>

          {/* Nút bấm Kích hoạt tìm kiếm */}
          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={loading}
              className="flex items-center gap-2 px-5 py-2 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-700 transition disabled:opacity-50 cursor-pointer shadow-xs"
            >
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Filter className="w-4 h-4" />}
              Áp dụng bộ lọc
            </button>
          </div>
        </form>

        {/* Bảng kết quả hiển thị dữ liệu Log */}
        <div className="bg-white rounded-xl shadow-xs border border-gray-200 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200">
                  <th className="p-4 text-xs font-bold text-gray-500 uppercase w-48">Thời gian</th>
                  <th className="p-4 text-xs font-bold text-gray-500 uppercase w-32">Đối tượng</th>
                  <th className="p-4 text-xs font-bold text-gray-500 uppercase w-36">Hành động</th>
                  <th className="p-4 text-xs font-bold text-gray-500 uppercase">Câu lệnh truy vấn (Query Statement)</th>
                  <th className="p-4 text-xs font-bold text-gray-500 uppercase w-28 text-center">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {loading ? (
                  <tr>
                    <td colSpan={5} className="text-center p-12 text-gray-500 font-medium">
                      <div className="flex items-center justify-center gap-2">
                        <Loader2 className="w-5 h-5 animate-spin text-indigo-600" />
                        Đang thực hiện truy vấn Full-text search trên Elastic...
                      </div>
                    </td>
                  </tr>
                ) : logs.length > 0 ? (
                  logs.map((log, idx) => (
                    <tr key={log.id || idx} className="hover:bg-gray-50/50 transition">
                      <td className="p-4 text-sm font-mono text-gray-600 whitespace-nowrap">
                        {new Date(log.timestamp).toLocaleString('vi-VN')}
                      </td>
                      <td className="p-4 text-sm font-semibold text-gray-700">
                        {log.actor}
                      </td>
                      <td className="p-4 text-sm">
                        <span className="px-2 py-0.5 rounded text-xs font-medium bg-blue-50 text-blue-700 border border-blue-100 uppercase">
                          {log.actionType}
                        </span>
                      </td>
                      {/* 2. Thay đổi phần render từ log.description sang log.queryStatement và bọc font-mono nhìn cho giống code SQL */}
                      <td className="p-4 text-sm text-slate-700 font-mono max-w-xl break-words bg-slate-50/50">
                        {log.queryStatement || <span className="text-gray-400 italic">N/A</span>}
                      </td>
                      <td className="p-4 text-sm text-center whitespace-nowrap">
                        <span className={`inline-block px-2 py-0.5 text-xs font-semibold rounded-full ${
                          log.status?.toLowerCase() === 'success' || log.status?.toLowerCase() === 'true'
                            ? 'bg-green-50 text-green-700 border border-green-200'
                            : 'bg-red-50 text-red-700 border border-red-200'
                        }`}>
                          {log.status || 'SUCCESS'}
                        </span>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5} className="text-center p-12 text-gray-400">
                      Không tìm thấy kết quả log nào khớp với điều kiện lọc hiện tại.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Thanh phân trang phía dưới bảng */}
          {totalPages > 1 && (
            <div className="bg-gray-50 px-4 py-3 border-t border-gray-200 flex items-center justify-between sm:px-6">
              <div className="text-sm text-gray-500">
                Hiển thị kết quả từ <span className="font-semibold">{page * size + 1}</span> đến{' '}
                <span className="font-semibold">{Math.min((page + 1) * size, totalElements)}</span> trong tổng số{' '}
                <span className="font-semibold">{totalElements}</span> logs.
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => handleSearch(page - 1)}
                  disabled={page === 0 || loading}
                  className="p-1.5 rounded-lg border border-gray-300 bg-white hover:bg-gray-50 text-gray-600 disabled:opacity-50 cursor-pointer"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  onClick={() => handleSearch(page + 1)}
                  disabled={page >= totalPages - 1 || loading}
                  className="p-1.5 rounded-lg border border-gray-300 bg-white hover:bg-gray-50 text-gray-600 disabled:opacity-50 cursor-pointer"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>

      </div>
    </div>
  );
}