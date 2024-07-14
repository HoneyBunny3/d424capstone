package com.example.d424capstone.database;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.example.d424capstone.dao.*;
import com.example.d424capstone.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Repository {
    private final UserDAO userDAO;
    private final CatDAO catDAO;
    private final StoreItemDAO storeItemDAO;
    private final CartItemDAO cartItemDAO;
    private final OrderDAO orderDAO;
    private final SocialPostDAO socialPostDAO;
    private final PremiumStorefrontDAO premiumStorefrontDAO;
    private final ContactMessageDAO contactMessageDAO;
    private final SharedPreferences sharedPreferences;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public Repository(Application application) {
        DatabaseBuilder db = DatabaseBuilder.getDatabase(application);
        userDAO = db.userDAO();
        catDAO = db.catDAO();
        storeItemDAO = db.storeItemDAO();
        cartItemDAO = db.cartItemDAO();
        orderDAO = db.orderDAO();
        socialPostDAO = db.socialPostDAO();
        premiumStorefrontDAO = db.premiumStorefrontDAO();
        contactMessageDAO = db.contactMessageDAO();
        sharedPreferences = application.getSharedPreferences("UserPrefs", Application.MODE_PRIVATE);

        populateInitialData(application.getApplicationContext());
        preloadStoreItems();
//        preloadPremiumStoreItems();
    }

    private void populateInitialData(Context context) {
        databaseWriteExecutor.execute(() -> {
            if (userDAO.getAllUsers().isEmpty()) {
                Handler handler = new Handler(Looper.getMainLooper());

                User adminUser = new User(0, "Admin", "Hearthy", "hearthy@example.com", "1234567890", "!234Abcd", "ADMIN");
                User premiumUser = new User(0, "Premium", "Shadow", "shadow@example.com", "1234567890", "!234Abcd", "PREMIUM");
                User regularUser = new User(0, "Regular", "Donut", "donut@example.com", "1234567890", "!234Abcd", "REGULAR");

                long adminUserId = userDAO.insert(adminUser);
                long premiumUserId = userDAO.insert(premiumUser);
                long regularUserId = userDAO.insert(regularUser);

                Cat premiumCat = new Cat(0, "Socks", 3, "", "Friendly cat", (int) premiumUserId);
                Cat regularCat = new Cat(0, "Clover", 1, "", "Adventurous cat", (int) regularUserId);

                catDAO.insert(premiumCat);
                catDAO.insert(regularCat);
            }
            preloadSocialPosts();
        });
    }

    private void preloadStoreItems() {
        databaseWriteExecutor.execute(() -> {
            if (storeItemDAO.getAllStoreItems().isEmpty()) {
                storeItemDAO.insert(new StoreItem(0, "Cat Toy", "Fun toy for cats", 9.99, true, false));
                storeItemDAO.insert(new StoreItem(0, "Cat Bed", "Comfortable bed for cats", 29.99, false, true)); //premium product
                storeItemDAO.insert(new StoreItem(0, "Cat Food", "Nutritious food for cats", 19.99, false, false));
                storeItemDAO.insert(new StoreItem(0, "Cat Scratcher", "Durable scratcher for cats", 14.99, false, false));
                storeItemDAO.insert(new StoreItem(0, "Cat Litter", "Odor-free cat litter", 10.99, false, false));
            }
        });
    }

    private void preloadSocialPosts() {
        databaseWriteExecutor.execute(() -> {
            if (socialPostDAO.getAllSocialPosts().isEmpty()) {
                socialPostDAO.insert(new SocialPost(0, 1, "Enjoying the sun with my cat!", 10));
                socialPostDAO.insert(new SocialPost(0, 2, "My cat's new favorite toy!", 25));
                socialPostDAO.insert(new SocialPost(0, 1, "Cat naps are the best naps.", 15));
            }
        });
    }

    // Premium Storefront methods
    public void insertPremiumStorefront(PremiumStorefront storefront) {
        databaseWriteExecutor.execute(() -> premiumStorefrontDAO.insert(storefront));
    }

    public void updatePremiumStorefront(PremiumStorefront storefront) {
        databaseWriteExecutor.execute(() -> premiumStorefrontDAO.update(storefront));
    }

    public void deletePremiumStorefront(int storefrontID) {
        databaseWriteExecutor.execute(() -> {
            PremiumStorefront storefront = premiumStorefrontDAO.getStorefrontByID(storefrontID);
            if (storefront != null) {
                premiumStorefrontDAO.delete(storefront);
            }
        });
    }

    public List<PremiumStorefront> getPremiumStorefrontsByUserID(int userID) {
        final List<PremiumStorefront>[] storefronts = new List[1];
        CountDownLatch latch = new CountDownLatch(1);
        databaseWriteExecutor.execute(() -> {
            storefronts[0] = premiumStorefrontDAO.getStorefrontsByUserID(userID);
            latch.countDown();
        });
        try {
            latch.await(); // Wait for the database operation to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return storefronts[0];
    }

    // User-related methods
    public User getUserByID(int userID) {
        final User[] user = new User[1];
        CountDownLatch latch = new CountDownLatch(1);
        databaseWriteExecutor.execute(() -> {
            user[0] = userDAO.getUserByID(userID);
            latch.countDown();
        });
        try {
            latch.await(); // Wait for the database operation to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return user[0];
    }

    public List<User> getUsersForCat(int catID) {
        final List<User>[] users = new List[1];
        databaseWriteExecutor.execute(() -> users[0] = userDAO.getUsersForCat(catID));
        return users[0];
    }

    public List<User> getAllUsers() {
        final List<User>[] users = new List[1];
        CountDownLatch latch = new CountDownLatch(1);
        databaseWriteExecutor.execute(() -> {
            users[0] = userDAO.getAllUsers();
            latch.countDown();
        });
        try {
            latch.await(); // Wait for the database operation to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (users[0] == null) {
            users[0] = new ArrayList<>(); // Ensure the list is initialized
        }
        return users[0];
    }

    public void insertUser(User user) {
        databaseWriteExecutor.execute(() -> userDAO.insert(user));
    }

    public void updateUser(User user) {
        databaseWriteExecutor.execute(() -> userDAO.update(user));
    }

    public void deleteUser(int userID) {
        databaseWriteExecutor.execute(() -> {
            User user = userDAO.getUserByID(userID);
            if (user != null) {
                userDAO.delete(user);
            }
        });
    }

    public User getUserByEmail(String email) {
        final User[] user = new User[1];
        databaseWriteExecutor.execute(() -> user[0] = userDAO.getUserByEmail(email));
        try {
            Thread.sleep(500); // Small delay to ensure background execution
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return user[0];
    }

    public User getCurrentUser() {
        int userID = sharedPreferences.getInt("LoggedInUserID", -1);
        if (userID != -1) {
            return getUserByID(userID);
        }
        return null;
    }

    // StoreItem-related methods
    public List<StoreItem> getAllStoreItems() {
        final List<StoreItem>[] items = new List[1];
        CountDownLatch latch = new CountDownLatch(1);
        databaseWriteExecutor.execute(() -> {
            items[0] = storeItemDAO.getAllStoreItems();
            latch.countDown();
        });
        try {
            latch.await(); // Wait for the database operation to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return items[0];
    }

    public void insertStoreItem(StoreItem storeItem) {
        databaseWriteExecutor.execute(() -> storeItemDAO.insert(storeItem));
    }

    public void updateStoreItem(StoreItem storeItem) {
        databaseWriteExecutor.execute(() -> storeItemDAO.update(storeItem));
    }

    public void deleteStoreItem(int storeItemID) {
        databaseWriteExecutor.execute(() -> storeItemDAO.delete(storeItemID));
    }

    public StoreItem getFeaturedItem() {
        final StoreItem[] item = new StoreItem[1];
        databaseWriteExecutor.execute(() -> item[0] = storeItemDAO.getFeaturedItem());
        return item[0];
    }

    // Cat-related methods
    public Cat getCatByID(int catID) {
        final Cat[] cat = new Cat[1];
        CountDownLatch latch = new CountDownLatch(1);
        databaseWriteExecutor.execute(() -> {
            cat[0] = catDAO.getCatByID(catID);
            latch.countDown();
        });
        try {
            latch.await(); // Wait for the database operation to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return cat[0];
    }

    public List<Cat> getCatsForUser(int userID) {
        final List<Cat>[] cats = new List[1];
        CountDownLatch latch = new CountDownLatch(1);
        databaseWriteExecutor.execute(() -> {
            cats[0] = catDAO.getCatsForUser(userID);
            latch.countDown();
        });
        try {
            latch.await(); // Wait for the database operation to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return cats[0];
    }

    public long insertCat(Cat cat) {
        final long[] id = new long[1];
        databaseWriteExecutor.execute(() -> id[0] = catDAO.insert(cat));
        return id[0];
    }

    public void updateCat(Cat cat) {
        databaseWriteExecutor.execute(() -> catDAO.update(cat));
    }

    public void deleteCat(int catID) {
        databaseWriteExecutor.execute(() -> {
            Cat cat = catDAO.getCatByID(catID);
            if (cat != null) {
                catDAO.delete(cat);
            }
        });
    }

    // CartItem-related methods
    public List<CartItem> getAllCartItems() {
        List<CartItem> cartItems = cartItemDAO.getAllCartItems();
        if (cartItems == null) {
            cartItems = new ArrayList<>();
        }
        return cartItems;
    }

    public void insertCartItem(CartItem cartItem) {
        databaseWriteExecutor.execute(() -> cartItemDAO.insert(cartItem));
    }

    public void updateCartItem(CartItem cartItem) {
        databaseWriteExecutor.execute(() -> cartItemDAO.update(cartItem));
    }

    public void deleteCartItem(int cartItemID) {
        databaseWriteExecutor.execute(() -> cartItemDAO.delete(cartItemID));
    }

    public void clearCartItems() {
        databaseWriteExecutor.execute(cartItemDAO::clearAll);
    }

    // Order-related methods
    public void insertOrder(Order order) {
        databaseWriteExecutor.execute(() -> orderDAO.insert(order));
    }

    public void updateOrder(Order order) {
        databaseWriteExecutor.execute(() -> orderDAO.update(order));
    }

    public Order getLatestOrder() {
        final Order[] order = new Order[1];
        databaseWriteExecutor.execute(() -> order[0] = orderDAO.getLatestOrder());
        return order[0];
    }

    public List<Order> getAllOrders() {
        final List<Order>[] orders = new List[1];
        CountDownLatch latch = new CountDownLatch(1);
        databaseWriteExecutor.execute(() -> orders[0] = orderDAO.getAllOrders());
        try {
            latch.await(); // Wait for the database operation to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return orders[0];
    }

    public void insertOrderForCurrentUser(Order order) {
        int userID = sharedPreferences.getInt("LoggedInUserID", -1);
        if (userID != -1) {
            order.setUserID(userID);
            insertOrder(order);
        }
    }

    public Order getLatestOrderForUser(int userID) {
        final Order[] order = new Order[1];
        databaseWriteExecutor.execute(() -> {
            order[0] = orderDAO.getLatestOrderForUser(userID);
        });
        try {
            Thread.sleep(500); // Small delay to ensure background execution
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return order[0];
    }

    public List<Order> getOrdersByUserID(int userID) {
        final List<Order>[] orders = new List[1];
        CountDownLatch latch = new CountDownLatch(1);
        databaseWriteExecutor.execute(() -> {
            orders[0] = orderDAO.getOrdersByUserId(userID);
            latch.countDown();
        });
        try {
            latch.await(); // Wait for the database operation to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return orders[0];
    }

    public Order getOrderByID(int orderID) {
        final Order[] order = new Order[1];
        CountDownLatch latch = new CountDownLatch(1);
        databaseWriteExecutor.execute(() -> {
            order[0] = orderDAO.getOrderById(orderID);
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return order[0];
    }

    // SocialPost-related methods
    public List<SocialPost> getAllSocialPosts() {
        final List<SocialPost>[] posts = new List[1];
        databaseWriteExecutor.execute(() -> posts[0] = socialPostDAO.getAllSocialPosts());
        try {
            Thread.sleep(500); // Small delay to ensure background execution
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return posts[0] != null ? posts[0] : new ArrayList<>();
    }

    public void insertSocialPost(SocialPost socialPost) {
        databaseWriteExecutor.execute(() -> socialPostDAO.insert(socialPost));
    }

    public void updateSocialPost(SocialPost socialPost) {
        databaseWriteExecutor.execute(() -> socialPostDAO.update(socialPost));
    }

    public void deleteSocialPost(int socialPostID) {
        databaseWriteExecutor.execute(() -> socialPostDAO.delete(socialPostID));
    }

    public void likePost(int userID, SocialPost socialPost) {
        User currentUser = getUserByID(userID);
        if (currentUser != null && !currentUser.hasLikedPost(socialPost.getSocialPostID())) {
            socialPost.incrementLikes();
            currentUser.addLikedPost(socialPost.getSocialPostID());
            databaseWriteExecutor.execute(() -> {
                userDAO.update(currentUser);
                socialPostDAO.update(socialPost);
            });
        }
    }

    public void insertContactMessage(ContactMessage contactMessage) {
        databaseWriteExecutor.execute(() -> contactMessageDAO.insert(contactMessage));
    }

    public List<ContactMessage> getAllContactMessages() {
        final List<ContactMessage>[] messages = new List[1];
        CountDownLatch latch = new CountDownLatch(1);
        databaseWriteExecutor.execute(() -> {
            messages[0] = contactMessageDAO.getAllMessages();
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return messages[0];
    }

    public List<StoreItem> searchStoreItems(String query) {
        final List<StoreItem>[] items = new List[1];
        databaseWriteExecutor.execute(() -> {
            items[0] = storeItemDAO.searchStoreItems("%" + query + "%");
        });
        try {
            Thread.sleep(500); // Small delay to ensure background execution
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return items[0];
    }

    public List<SocialPost> searchSocialPosts(String query) {
        final List<SocialPost>[] posts = new List[1];
        databaseWriteExecutor.execute(() -> {
            posts[0] = socialPostDAO.searchSocialPosts("%" + query + "%");
        });
        try {
            Thread.sleep(500); // Small delay to ensure background execution
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return posts[0];
    }
}