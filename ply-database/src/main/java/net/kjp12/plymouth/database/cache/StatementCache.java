package net.kjp12.plymouth.database.cache;// Created 2021-05-06T15:00:03

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kjp12.plymouth.database.PlymouthException;
import net.kjp12.plymouth.database.records.LookupRecord;
import net.kjp12.plymouth.database.records.PlymouthRecord;
import org.objectweb.asm.*;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.locks.StampedLock;

/**
 * @author KJP12
 * @since ${version}
 **/
public final class StatementCache<I extends LookupRecord<O>, O extends PlymouthRecord> { // I - Input | O - Output
    private static final MethodHandles.Lookup SELF = MethodHandles.lookup();

    private static final String
            STATEMENT_TYPE = Type.getInternalName(PreparedStatement.class),
            STATEMENT_DESCRIPTOR = Type.getDescriptor(PreparedStatement.class),
            RESULT_SET_TYPE = Type.getInternalName(ResultSet.class),
            RESULT_SET_DESCRIPTOR = Type.getDescriptor(ResultSet.class);

    private static final Type
            STRING_TYPE = Type.getType(String.class);

    private final SqlConnectionProvider sqlImpl;
    private final StampedLock lock = new StampedLock();
    private final Int2ObjectMap<StatementHandler<I, O>> statementCache = new Int2ObjectOpenHashMap<>();
    private final Method proxy;
    private final Class<I> iClass;

    public StatementCache(SqlConnectionProvider sqlImpl, Class<I> iClass, Method proxy) {
        this.sqlImpl = sqlImpl;
        this.iClass = iClass;
        this.proxy = proxy;
    }

    /**
     * Closes all prepared statements within the cache.
     *
     * @throws PlymouthException if any non-SQL-related exception gets thrown while processing.
     */
    public void reload() throws PlymouthException {
        var w = lock.writeLock();
        ArrayList<PlymouthException> l = null;
        try {
            var itr = Int2ObjectMaps.fastIterator(statementCache);
            while (itr.hasNext()) {
                var e = itr.next();
                try {
                    e.getValue().closeStatement();
                } catch (PlymouthException sql) {
                    if (l == null) l = new ArrayList<>();
                    l.add(new PlymouthException(sql, e));
                }
            }
            if (l != null) {
                // This allows a bit smarter of stack omission by having one carrier to suppress the multiple stacks.
                var t = new Throwable("Issues closing multiple prepared statements...");
                for (var p : l) t.addSuppressed(p);
                t.printStackTrace();
            }
        } catch (Exception e) {
            if (l != null) {
                for (var p : l) e.addSuppressed(p);
            }
            var msg = statementCache.toString();
            throw new PlymouthException(e, msg);
        } finally {
            statementCache.clear();
            lock.unlockWrite(w);
        }
    }

    public void handle(I i) throws PlymouthException {
        statementCache.computeIfAbsent(i.flags(), j -> surrogate(i)).query(i);
    }

    private StatementHandler<I, O> surrogate(I i) throws PlymouthException {
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        var handlerType = Type.getType(StatementHandler.class);
        var iType = Type.getType(iClass);
        var iDesc = Type.getDescriptor(iClass);
        var iName = Type.getInternalName(iClass);
        var provider = Type.getType(SqlConnectionProvider.class);
        var statementHandler = handlerType.getInternalName();
        var self = statementHandler + '$' + i.getType() + '$' + i.flags();
        var sqlQuery = new StringBuilder();
        byte[] array;

        Table[] tables = proxy.getAnnotationsByType(Table.class);
        Arrays.sort(tables, Comparator.comparingInt(Table::table));

        writer.visit(Opcodes.V11, /*Opcodes.ACC_SYNTHETIC |*/ Opcodes.ACC_FINAL, self, null, statementHandler, null);

        { // Query function, overrides the interface.
            var qDesc = Type.getMethodDescriptor(Type.VOID_TYPE, iType);
            var synth = writer.visitMethod(Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE, "query", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(LookupRecord.class)), null, null);
            synth.visitVarInsn(Opcodes.ALOAD, 0);
            synth.visitVarInsn(Opcodes.ALOAD, 1);
            synth.visitTypeInsn(Opcodes.CHECKCAST, iName);
            synth.visitMethodInsn(Opcodes.INVOKEVIRTUAL, self, "query", qDesc, false);
            synth.visitInsn(Opcodes.RETURN);
            synth.visitMaxs(0, 0);
            synth.visitEnd();

            var query = writer.visitMethod(Opcodes.ACC_FINAL, "query", qDesc, null, new String[]{Type.getInternalName(PlymouthException.class)});

            // Execute the query then store at 2, replacing the statement.
            query.visitVarInsn(Opcodes.ALOAD, 0);
            query.visitVarInsn(Opcodes.ALOAD, 1);
            query.visitMethodInsn(Opcodes.INVOKEVIRTUAL, self, "submit", '(' + iDesc + ')' + RESULT_SET_DESCRIPTOR, false);
            query.visitVarInsn(Opcodes.ASTORE, 2);

            // Create an array list then store at 3. Generics not required.
            query.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
            query.visitInsn(Opcodes.DUP);
            query.visitVarInsn(Opcodes.ALOAD, 2);
            query.visitMethodInsn(Opcodes.INVOKEINTERFACE, RESULT_SET_TYPE, "getFetchSize", "()I", true);
            query.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "(I)V", false);
            query.visitVarInsn(Opcodes.ASTORE, 3);

            // Setup loop
            Label loop = new Label(), end = new Label();
            query.visitLabel(loop);
            query.visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
            query.visitVarInsn(Opcodes.ALOAD, 2);
            query.visitMethodInsn(Opcodes.INVOKEINTERFACE, RESULT_SET_TYPE, "next", "()Z", true);
            query.visitJumpInsn(Opcodes.IFEQ, end);

            // Loop
            query.visitVarInsn(Opcodes.ALOAD, 3);

            { // Writes the selections from the tables.
                sqlQuery.append("select ");
                var annots = proxy.getParameterAnnotations();
                var params = proxy.getParameterTypes();
                for (int a = 0, l = params.length, c; a < l; a++) {
                    Value value = null;
                    for (var b : annots[a]) {
                        if (b instanceof Value v) {
                            value = v;
                            break;
                        }
                    }
                    if (value == null)
                        throw new IllegalArgumentException(proxy + " does not contain Value annotation on parameter " + i + ": param: " + params[a] + ", annotations: " + Arrays.toString(annots[a]));

                    appendQuery(sqlQuery, value.table(), value.value());

                    query.visitVarInsn(Opcodes.ALOAD, 2);
                    int stack = a + 1;
                    if (stack <= 5)
                        // Use the single-instruction opcodes where applicable.
                        query.visitInsn(Opcodes.ICONST_0 + stack);
                    else
                        query.visitIntInsn(Opcodes.BIPUSH, stack);

                    var clazz = params[a];
                    var mapper = ClassMap.findMapper(clazz);
                    if (mapper.passClass) {
                        var type = Type.getType(clazz);
                        query.visitLdcInsn(type);
                        query.visitMethodInsn(Opcodes.INVOKEINTERFACE, RESULT_SET_TYPE, mapper.getter, "(ILjava/lang/Class;)Ljava/lang/Object;", true);
                        query.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
                    } else {
                        query.visitMethodInsn(Opcodes.INVOKEINTERFACE, RESULT_SET_TYPE, mapper.getter, "(I)" + mapper.internal.descriptorString(), true);
                    }
                }
                sqlQuery.setLength(sqlQuery.length() - 1);
            }

            { // Writes the from tables
                for (Table table : tables) {
                    if (table.table() == 0) {
                        sqlQuery.append(" from ").append(table.value()).append(' ');
                    } else {
                        var match = table.match();
                        char assigned = (char) ('`' + table.table());
                        sqlQuery.append("left outer join ").append(table.value()).append(' ').append(assigned)
                                .append(" on(").append(match.primary()).append('=')
                                .append(assigned).append('.').append(match.secondary()).append(')');
                    }
                }
            }

            query.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(proxy.getDeclaringClass()), proxy.getName(), Type.getMethodDescriptor(proxy), false);
            query.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);
            query.visitInsn(Opcodes.POP);
            query.visitJumpInsn(Opcodes.GOTO, loop);

            // End loop & method, completes
            query.visitLabel(end);
            query.visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
            query.visitVarInsn(Opcodes.ALOAD, 1);
            query.visitVarInsn(Opcodes.ALOAD, 3);
            query.visitMethodInsn(Opcodes.INVOKEVIRTUAL, iName, "complete", "(Ljava/util/List;)V", false);
            query.visitInsn(Opcodes.RETURN);
            query.visitMaxs(0, 0);
            query.visitEnd();
        }
        { // internal helper 'submit'
            var submit = writer.visitMethod(/*Opcodes.ACC_SYNTHETIC |*/ Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "submit", Type.getMethodDescriptor(Type.getType(ResultSet.class), iType), null, new String[]{Type.getInternalName(PlymouthException.class), Type.getInternalName(SQLException.class)});
            submit.visitVarInsn(Opcodes.ALOAD, 0);
            submit.visitFieldInsn(Opcodes.GETFIELD, self, "statement", STATEMENT_DESCRIPTOR);
            submit.visitVarInsn(Opcodes.ASTORE, 2);

            // JVM is a stack machine; only the amount of params necessary will be taken from the stack.
            // Writes the query. This also writes the query instructions.
            C0 c0 = new C0(iClass, sqlImpl.getClass());
            {
                Query[] classQueries = proxy.getDeclaringClass().getAnnotationsByType(Query.class),
                        proxyQueries = proxy.getAnnotationsByType(Query.class),
                        totalQueries = new Query[classQueries.length + proxyQueries.length];
                System.arraycopy(classQueries, 0, totalQueries, 0, classQueries.length);
                System.arraycopy(proxyQueries, 0, totalQueries, classQueries.length, proxyQueries.length);
                boolean ran = false;
                for (var q : totalQueries) {
                    var check = i.flags() & q.mask();
                    if (check != (q.maskRq() == -1 ? q.mask() : q.maskRq())) continue;
                    if (!ran) {
                        ran = true;
                        sqlQuery.append("where ");
                    } else {
                        sqlQuery.append(" and ");
                    }
                    sqlQuery.append(q.query());

                    for (var v : q.values())
                        try {
                            c0.compile(submit, v);
                        } catch (Throwable roe) {
                            throw new PlymouthException(roe, v, q, c0, i, sqlQuery, sqlImpl);
                        }
                }
            }
            {
                Pagination pagination = proxy.getAnnotation(Pagination.class);
                var sort = pagination.sort();
                var sortValue = sort.value();
                if (sortValue.length != 0) {
                    appendQuery(sqlQuery.append(" order by "), sort.table(), sortValue);
                    var l = sqlQuery.length();
                    sqlQuery.replace(l - 1, l, " desc");
                }
                var limit = pagination.limit();
                if (!limit.isBlank()) {
                    sqlQuery.append(" limit ?");
                    try {
                        c0.compile(submit, limit);
                    } catch (Throwable roe) {
                        throw new PlymouthException(roe, limit, pagination, sqlQuery, sqlImpl);
                    }
                }
                var offset = pagination.offset();
                if (!offset.isBlank()) {
                    sqlQuery.append(" offset ?");
                    try {
                        c0.compile(submit, offset);
                    } catch (Throwable roe) {
                        throw new PlymouthException(roe, offset, pagination, sqlQuery, sqlImpl);
                    }
                }
            }

            submit.visitVarInsn(Opcodes.ALOAD, 2);
            submit.visitMethodInsn(Opcodes.INVOKEINTERFACE, STATEMENT_TYPE, "executeQuery", "()" + RESULT_SET_DESCRIPTOR, true);
            submit.visitInsn(Opcodes.ARETURN);
            submit.visitMaxs(0, 0);
            submit.visitEnd();
        }
        { // constructor
            var init = writer.visitMethod(0, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, provider), null, null);
            init.visitVarInsn(Opcodes.ALOAD, 0);
            init.visitVarInsn(Opcodes.ALOAD, 1);
            init.visitLdcInsn(sqlQuery.toString());
            // Cache type has to be passed else it is impossible to initialise the class otherwise.
            init.visitMethodInsn(Opcodes.INVOKESPECIAL, statementHandler, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, provider, STRING_TYPE), false);
            init.visitInsn(Opcodes.RETURN);
            init.visitMaxs(3, 1);
            init.visitEnd();
        }
        writer.visitEnd();
        array = writer.toByteArray();
        try {
            // Allow for trivial debugging and decompilation of the handler if it severely broke.
            if (Boolean.parseBoolean(System.getProperty("statement.cache.dump")))
                try (var out = new FileOutputStream("scasm-dump-" + System.nanoTime() + ".class")) {
                    out.write(array);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            // Ensures that the statement handler is entirely initialised before going on to the hidden class.
            SELF.ensureInitialized(StatementHandler.class);
            var nest = SELF.defineHiddenClass(array, true);
            var cons = nest.findConstructor(nest.lookupClass(), MethodType.methodType(void.class, SqlConnectionProvider.class));
            @SuppressWarnings("unchecked") var inst = (StatementHandler<I, O>) cons.invoke(sqlImpl);
            return inst;
        } catch (VirtualMachineError | LinkageError | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException |
                InstantiationException | InvocationTargetException | NullPointerException | SQLException | PlymouthException roe) {
            Throwable suppressed = null;
            try (var out = new FileOutputStream("scasm.class")) {
                out.write(array);
            } catch (Exception e) {
                suppressed = e;
            }
            var asm = new ASMifier();
            var sw = new StringWriter();
            var spw = new PrintWriter(sw);
            var trc = new TraceClassVisitor(null, asm, spw);
            new ClassReader(array).accept(trc, ClassReader.EXPAND_FRAMES);
            var pe = new PlymouthException(roe, i, sqlQuery, sw, "Bytecode has been dumped at scasm.class.");
            if (suppressed != null) pe.addSuppressed(suppressed);
            if (roe instanceof VirtualMachineError) throw new Error("Virtual machine has failed.", pe);
            throw pe;
        } catch (Throwable throwable) {
            throw new AssertionError("This should never occur.", throwable);
        }
    }

    private static void appendQuery(StringBuilder sqlQuery, int table, String[] nameStack) {
        int c = sqlQuery.length();
        if (table == 0) {
            sqlQuery.append(nameStack[0]);
        } else {
            sqlQuery.append((char) ('`' + table)).append('.').append(nameStack[0]);
        }
        for (int v = 1; v < nameStack.length; v++) {
            sqlQuery.insert(c, '(').append(").").append(nameStack[v]);
        }
        sqlQuery.append(',');
    }
}
