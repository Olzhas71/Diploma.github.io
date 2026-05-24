import { useEffect, useRef, useState } from 'react';
import type { SpotStatusEvent } from '@/types';

export interface OccupancyWebSocketStatus {
  connected: boolean;
  lastEventAt: number | null;
}

export function useOccupancyWebSocket(
  handler: (event: SpotStatusEvent) => void
): OccupancyWebSocketStatus {
  const handlerRef = useRef(handler);
  handlerRef.current = handler;
  const [connected, setConnected] = useState(false);
  const [lastEventAt, setLastEventAt] = useState<number | null>(null);

  useEffect(() => {
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const url = `${proto}://${window.location.host}/ws/occupancy`;
    let socket: WebSocket | null = null;
    let reconnectTimer: number | null = null;
    let stopped = false;

    const connect = () => {
      socket = new WebSocket(url);
      socket.onopen = () => setConnected(true);
      socket.onmessage = (msg) => {
        try {
          const data = JSON.parse(msg.data) as SpotStatusEvent;
          if (data.type === 'SPOT_STATUS') {
            setLastEventAt(Date.now());
            handlerRef.current(data);
          }
        } catch {
          // ignore malformed frames
        }
      };
      socket.onclose = () => {
        setConnected(false);
        if (!stopped) reconnectTimer = window.setTimeout(connect, 2000);
      };
      socket.onerror = () => socket?.close();
    };

    connect();
    return () => {
      stopped = true;
      if (reconnectTimer) clearTimeout(reconnectTimer);
      socket?.close();
    };
  }, []);

  return { connected, lastEventAt };
}
