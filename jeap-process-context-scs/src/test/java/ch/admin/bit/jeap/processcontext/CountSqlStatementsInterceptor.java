package ch.admin.bit.jeap.processcontext;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class CountSqlStatementsInterceptor implements StatementInspector {

    private static final AtomicInteger insertCount = new AtomicInteger();
    private static final AtomicInteger updateCount = new AtomicInteger();
    private static final AtomicInteger selectCount = new AtomicInteger();
    private static final AtomicInteger deleteCount = new AtomicInteger();

    static {
        clearCounts();
    }

    @Override
    public String inspect(String sql) {
        if (sql.startsWith("select ")) {
            selectCount.incrementAndGet();
        } else if (sql.startsWith("insert ")) {
            insertCount.incrementAndGet();
        } else if (sql.startsWith("update ")) {
            updateCount.incrementAndGet();
        } else if (sql.startsWith("delete ")) {
            deleteCount.incrementAndGet();
        }
        return sql;
    }

    public static void clearCounts() {
        insertCount.set(0);
        updateCount.set(0);
        selectCount.set(0);
        deleteCount.set(0);
    }

    public static void logCounts() {
        long selects = selectCount.get();
        long inserts = insertCount.get();
        long updates = updateCount.get();
        long deletes = deleteCount.get();
        long statements = selects + inserts + updates + deletes;
        log.info("#statements: {}, #selects: {}, #inserts: {}, #updates: {}, #deletes: {}.",
                statements, selects, inserts, updates, deletes);
    }

    public static long getCountTotal() {
        return selectCount.get() + insertCount.get() + updateCount.get() + deleteCount.get();
    }

}