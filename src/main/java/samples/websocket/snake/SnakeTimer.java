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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Sets up the timer for the multi-player snake game WebSocket example.
 */
public class SnakeTimer {

	private static final Log log = LogFactory.getLog(SnakeTimer.class);

	private static Timer gameTimer = null;

	private static final long TICK_DELAY = 100;

	private static final ConcurrentHashMap<Integer, Snake> snakes = new ConcurrentHashMap<Integer, Snake>();
	private static final List<Apple> food = new ArrayList<>();

	public static synchronized void addSnake(Snake snake) {
		if (snakes.size() == 0) {
			startTimer();
		}
		snakes.put(snake.getId(), snake);
	}

	public static Collection<Snake> getSnakes() {
		return Collections.unmodifiableCollection(snakes.values());
	}

	public static synchronized void removeSnake(Snake snake) {
		snakes.remove(snake.getId());
		if (snakes.size() == 0) {
			stopTimer();
		}
	}

	public static synchronized void tick() throws Exception {
		StringBuilder sb = new StringBuilder();
		for (Iterator<Snake> iterator = SnakeTimer.getSnakes().iterator(); iterator
				.hasNext();) {
			Snake snake = iterator.next();
			snake.update(SnakeTimer.getSnakes(), SnakeTimer.food);
			sb.append(snake.getLocationsJson());
			if (iterator.hasNext()) {
				sb.append(',');
			}
		}
		StringBuilder ab = new StringBuilder();
		for (Iterator<Apple> iterator = food.iterator(); iterator.hasNext();) {
			Apple apple = iterator.next();
			ab.append(apple.getLocationJson());
			if (iterator.hasNext()) {
				ab.append(',');
			}
		}
		broadcast(String.format("{'type': 'update', 'data' : [%s], 'food' : [%s]}", sb.toString(), ab.toString()));
	}

	public static void broadcast(String message) throws Exception {
//		System.out.println("Broadcasting: " + message);
		Collection<Snake> snakes = new CopyOnWriteArrayList<>(SnakeTimer.getSnakes());
		for (Snake snake : snakes) {
			try {
				snake.sendMessage(message);
			}
			catch (Throwable ex) {
				// if Snake#sendMessage fails the client is removed
				removeSnake(snake);
			}
		}
	}

	public static void startTimer() {
		gameTimer = new Timer(SnakeTimer.class.getSimpleName() + " Timer");
		gameTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					tick();
				}
				catch (Throwable ex) {
					log.error("Caught to prevent timer from shutting down", ex);
				}
			}
		}, TICK_DELAY, TICK_DELAY);
	}

	public static void stopTimer() {
		if (gameTimer != null) {
			gameTimer.cancel();
		}
	}

	public static synchronized void addApple() {
		food.add(new Apple());
	}

	public static synchronized void removeApple() {
		food.remove(food.size() - 1);

	}
}
