/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package samples.websocket.snake;

import java.util.Iterator;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class SnakeWebSocketHandler extends TextWebSocketHandler {

	private Snake snake;

	public SnakeWebSocketHandler() {
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		Object ip = session.getAttributes().get("ip");
		System.out.println(ip);
		this.snake = new Snake(session);
		SnakeTimer.addSnake(this.snake);
		SnakeTimer.addApple();
		StringBuilder sb = new StringBuilder();
		for (Iterator<Snake> iterator = SnakeTimer.getSnakes().iterator(); iterator
				.hasNext();) {
			Snake snake = iterator.next();
			sb.append(String.format("{id: %d, color: '%s', ip: '%s'}",
					snake.getId(), snake.getHexColor(), snake.getIp()));
			if (iterator.hasNext()) {
				sb.append(',');
			}
		}
		SnakeTimer
				.broadcast(String.format("{'type': 'join','data':[%s]}", sb.toString()));
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message)
			throws Exception {
		String payload = message.getPayload();
		if ("west".equals(payload)) {
			this.snake.setDirection(Direction.WEST);
		}
		else if ("north".equals(payload)) {
			this.snake.setDirection(Direction.NORTH);
		}
		else if ("east".equals(payload)) {
			this.snake.setDirection(Direction.EAST);
		}
		else if ("south".equals(payload)) {
			this.snake.setDirection(Direction.SOUTH);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
			throws Exception {
		SnakeTimer.removeSnake(this.snake);
		SnakeTimer.removeApple();
		SnakeTimer.broadcast(String.format("{'type': 'leave', 'id': %d}", snake.getId()));
	}
}
