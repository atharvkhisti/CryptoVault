import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { auditApi } from '../services/auditApi';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/Card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/Table';
import { Skeleton } from '../components/ui/Skeleton';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Search, Calendar, RefreshCw, Layers } from 'lucide-react';

export const Audit: React.FC = () => {
  const { user } = useAuth();
  
  // Search / Filters
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('ALL');
  
  // Date Range state
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [isFilteringRange, setIsFilteringRange] = useState(false);

  // Queries
  const { data: logsData, isLoading, refetch, isRefetching } = useQuery({
    queryKey: ['audit-logs', user?.id, isFilteringRange, startDate, endDate],
    queryFn: () => {
      if (isFilteringRange && startDate && endDate) {
        // Enforce ISO Date string endings
        const startISO = `${startDate}T00:00:00`;
        const endISO = `${endDate}T23:59:59`;
        return auditApi.getLogsByDateRange(startISO, endISO);
      }
      if (user?.role === 'ADMIN') {
        return auditApi.getAllLogs();
      }
      return auditApi.getUserLogs(user?.id || '');
    },
    enabled: !!user?.id,
  });

  const logs = logsData?.data || [];

  // Filter types based on logs loaded
  const eventTypes = ['ALL', ...Array.from(new Set(logs.map((l) => l.eventType)))];

  const filteredLogs = logs.filter((log) => {
    const matchesSearch =
      log.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.action.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.serviceName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.ipAddress.includes(searchTerm);

    const matchesType = typeFilter === 'ALL' || log.eventType === typeFilter;

    return matchesSearch && matchesType;
  });

  const handleApplyDateRange = (e: React.FormEvent) => {
    e.preventDefault();
    if (startDate && endDate) {
      setIsFilteringRange(true);
    }
  };

  const handleClearDateRange = () => {
    setStartDate('');
    setEndDate('');
    setIsFilteringRange(false);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between text-left">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-white m-0">Audit Logs</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Browse verified immutable records tracking security operations, logins, and ledger compliance.
          </p>
        </div>
        <Button
          variant="glass"
          size="sm"
          onClick={() => refetch()}
          className="flex items-center space-x-1"
          disabled={isLoading || isRefetching}
        >
          <RefreshCw size={14} className={isRefetching ? 'animate-spin' : ''} />
          <span>Refresh</span>
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Date Range & Ingress Filters */}
        <Card className="text-left self-start lg:col-span-1">
          <CardHeader>
            <CardTitle>Date Filter</CardTitle>
            <CardDescription>Limit logs to a specific banking statements calendar range</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleApplyDateRange} className="space-y-4">
              <Input
                label="Start Date"
                type="date"
                required
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
              <Input
                label="End Date"
                type="date"
                required
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
              <div className="space-y-2 pt-2">
                <Button type="submit" className="w-full text-xs" variant="primary">
                  <Calendar size={13} className="mr-1.5" />
                  Apply Filter
                </Button>
                {isFilteringRange && (
                  <Button
                    type="button"
                    onClick={handleClearDateRange}
                    className="w-full text-xs"
                    variant="outline"
                  >
                    Clear Filter
                  </Button>
                )}
              </div>
            </form>
          </CardContent>
        </Card>

        {/* Audit Logs Table */}
        <Card className="lg:col-span-3 text-left">
          <CardHeader>
            <CardTitle>Compliance Records</CardTitle>
            <CardDescription>
              {isFilteringRange
                ? `Showing logs from ${startDate} to ${endDate}`
                : 'Showing complete user ledger session logs'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {/* Search Input & Dropdown filters */}
            <div className="flex flex-col sm:flex-row gap-3 mb-6">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-3 text-zinc-500" size={16} />
                <input
                  type="text"
                  placeholder="Search action, description, IP, or service..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-9 pr-4 py-2 text-sm bg-zinc-900 border border-zinc-800 text-white rounded-md placeholder-zinc-500 focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>
              
              <div className="flex items-center space-x-2 shrink-0">
                <Layers size={14} className="text-zinc-500" />
                <select
                  value={typeFilter}
                  onChange={(e) => setTypeFilter(e.target.value)}
                  className="px-3 py-2 text-xs bg-zinc-900 border border-zinc-800 text-zinc-400 rounded-md focus:outline-none focus:ring-1 focus:ring-primary"
                >
                  {eventTypes.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {isLoading ? (
              <div className="space-y-3">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            ) : filteredLogs.length === 0 ? (
              <div className="py-20 text-center text-xs text-muted-foreground border border-zinc-900 rounded-lg">
                No matching compliance logs recorded.
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Timestamp</TableHead>
                    <TableHead>Event Type</TableHead>
                    <TableHead>Service</TableHead>
                    <TableHead>IP Address</TableHead>
                    <TableHead>Description</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredLogs.map((log) => (
                    <TableRow key={log.auditId}>
                      <TableCell className="text-xs text-muted-foreground whitespace-nowrap">
                        {new Date(log.eventTimestamp).toLocaleString()}
                      </TableCell>
                      <TableCell className="font-semibold text-xs text-zinc-300">
                        {log.eventType}
                      </TableCell>
                      <TableCell className="font-mono text-xs text-zinc-500">
                        {log.serviceName}
                      </TableCell>
                      <TableCell className="font-mono text-xs text-zinc-400">
                        {log.ipAddress}
                      </TableCell>
                      <TableCell className="text-zinc-300 text-xs max-w-xs truncate">
                        {log.description}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
export default Audit;
