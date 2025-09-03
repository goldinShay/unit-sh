package ui.gui;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * A centralized utility for registering and navigating GUI pages using CardLayout.
 */
public class PageNavigator {

    private static JPanel container;                // Root panel containing all views
    private static CardLayout cardLayout;           // Layout manager for switching views

    private static final Map<Integer, String> pageMap = new HashMap<>();      // Page number → card name
    private static final Map<Integer, JComponent> pageRegistry = new HashMap<>(); // Page number → component

    private static final Stack<Integer> history = new Stack<>();              // Navigation history

    private static int currentPageId = -1;

    /**
     * Initializes the navigator with a root panel using CardLayout.
     */
    public static void initialize(JPanel rootPanel) {
        if (rootPanel.getLayout() instanceof CardLayout layoutInstance) {
            container = rootPanel;
            cardLayout = layoutInstance;
        } else {
            throw new IllegalArgumentException("PageNavigator requires a JPanel with CardLayout.");
        }
    }

    /**
     * Registers a new page with a specific ID. Replaces if already registered.
     */
    public static void registerPage(int pageNumber, JComponent page) {
        String pageKey = "PAGE_" + pageNumber;

        if (container == null) {
            throw new IllegalStateException("PageNavigator must be initialized before registering pages.");
        }

        if (pageMap.containsKey(pageNumber)) {
            container.remove(pageRegistry.get(pageNumber));
        }

        container.add(page, pageKey);
        pageMap.put(pageNumber, pageKey);
        pageRegistry.put(pageNumber, page);

        container.revalidate();
        container.repaint();

//        System.out.println("📄 Registered page " + pageNumber + " with " + page.getComponentCount() + " components.");
    }

    /**
     * Switches to a page by its numeric ID.
     */
    public static void goToPage(int pageNumber) {
        String pageKey = pageMap.get(pageNumber);
        if (pageKey != null && container != null && cardLayout != null) {
            cardLayout.show(container, pageKey);
            container.revalidate();
            container.repaint();
            currentPageId = pageNumber;

            // Push to history only if it's a new page
            if (history.isEmpty() || history.peek() != pageNumber) {
                history.push(pageNumber);
            }

            System.out.println("🔀 Switched to page " + pageNumber);
        } else {
            System.err.printf("❌ Page %d not found or PageNavigator not initialized.%n", pageNumber);
        }
    }

    /**
     * Navigates back to the previous page in history.
     */
    public static void goBack() {
        if (history.size() > 1) {
            history.pop(); // Remove current
            int previousPage = history.peek();
            goToPage(previousPage);
        } else {
            System.err.println("⚠️ No previous page to go back to.");
        }
    }

    /**
     * Checks if a page is already registered.
     */
    public static boolean isPageRegistered(int pageNumber) {
        return pageMap.containsKey(pageNumber);
    }

    /**
     * Returns the ID of the currently displayed page.
     */
    public static int getCurrentPageId() {
        return currentPageId;
    }

    /**
     * Returns the actual JPanel for a given page ID.
     */
    public static JComponent getPage(int pageNumber) {
        return pageRegistry.get(pageNumber);
    }
}