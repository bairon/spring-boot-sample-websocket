package samples.websocket.snake;

/**
 * Created by alsa on 05.10.2017.
 */
public class Apple {

    private Location place;

    public Apple() {
        this.place = SnakeUtils.getRandomLocation();
    }
    public void reset() {
        this.place = SnakeUtils.getRandomLocation();
    }

    public synchronized Location getPlace() {
        return this.place;
    }

    public synchronized String getLocationJson() {
        return String.format("{x: %d, y: %d}", this.place.x,
                this.place.y);
    }
}
