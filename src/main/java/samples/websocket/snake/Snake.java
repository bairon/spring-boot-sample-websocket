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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class Snake {

	private static final int DEFAULT_LENGTH = 5;

	private final int id;
	private final String ip;
	private final WebSocketSession session;

	private Direction direction;
	private int length = DEFAULT_LENGTH;
	private Location head;
	private final Deque<Location> tail = new ArrayDeque<Location>();
	private final String hexColor;

	public Snake(WebSocketSession session) {
		this.id = SnakeUtils.IDS.getAndIncrement();
		this.session = session;
		this.ip = session.getAttributes().get("ip").toString();
		this.hexColor = SnakeUtils.getRandomHexColor();
		resetState();
	}

	private void resetState() {
		this.direction = Direction.NONE;
		this.head = SnakeUtils.getRandomLocation();
		this.tail.clear();
		this.length = DEFAULT_LENGTH;
	}

	private synchronized void kill() throws Exception {
		resetState();
		sendMessage("{'type': 'dead'}");
	}

	private synchronized void reward(int n) throws Exception {
		this.length += n;
		sendMessage("{'type': 'feed'}");
	}

	protected void sendMessage(String msg) throws Exception {
		this.session.sendMessage(new TextMessage(msg));
	}

	public synchronized void update(Collection<Snake> snakes, List<Apple> food) throws Exception {
		Location nextLocation = this.head.getAdjacentLocation(this.direction);
		if (nextLocation.x >= SnakeUtils.PLAYFIELD_WIDTH) {
			nextLocation.x = 0;
		}
		if (nextLocation.y >= SnakeUtils.PLAYFIELD_HEIGHT) {
			nextLocation.y = 0;
		}
		if (nextLocation.x < 0) {
			nextLocation.x = SnakeUtils.PLAYFIELD_WIDTH;
		}
		if (nextLocation.y < 0) {
			nextLocation.y = SnakeUtils.PLAYFIELD_HEIGHT;
		}
		if (this.direction != Direction.NONE) {
			this.tail.addFirst(this.head);
			if (this.tail.size() > this.length) {
				this.tail.removeLast();
			}
			this.head = nextLocation;
		}

		handleCollisions(snakes);
		handleFeeding(food);
	}

	private void handleFeeding(List<Apple> food) throws Exception {
		for (Apple apple : food) {
			if (this.head.equals(apple.getPlace())) {
				this.reward(1);
				apple.reset();
			}
		}
	}

	private void handleCollisions(Collection<Snake> snakes) throws Exception {
		for (Snake snake : snakes) {
			boolean headCollision = this.id != snake.id
					&& snake.getHead().equals(this.head);
			boolean tailCollision = snake.getTail().contains(this.head);
			if (headCollision || tailCollision) {
				kill();
				if (this.id != snake.id) {
					snake.reward(2);
				}
			}
		}
	}

	public synchronized Location getHead() {
		return this.head;
	}

	public synchronized Collection<Location> getTail() {
		return this.tail;
	}

	public synchronized void setDirection(Direction direction) {
		this.direction = direction;
	}

	public synchronized String getLocationsJson() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("{x: %d, y: %d}", this.head.x,
				this.head.y));
		for (Location location : this.tail) {
			sb.append(',');
			sb.append(String.format("{x: %d, y: %d}", location.x,
					location.y));
		}
		return String.format("{'id':%d,'body':[%s]}", this.id,
				sb.toString());
	}

	public int getId() {
		return this.id;
	}

	public String getHexColor() {
		return this.hexColor;
	}

	public String getIp() {
		return ip;
	}
}
