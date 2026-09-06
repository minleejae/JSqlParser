/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.create.subscription;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.statement.replication.ReplicationOption;

public class SubscriptionOption extends ReplicationOption<SubscriptionOption.Kind> {
    public enum Kind {
        CONNECT, CREATE_SLOT, ENABLED, SLOT_NAME, BINARY, COPY_DATA, STREAMING, SYNCHRONOUS_COMMIT, TWO_PHASE, DISABLE_ON_ERROR, PASSWORD_REQUIRED, RUN_AS_OWNER, ORIGIN, FAILOVER, REFRESH, LSN
    }
    public enum Streaming {
        ON, OFF, PARALLEL
    }
    public enum Origin {
        ANY, NONE
    }
    public enum SynchronousCommit {
        OFF, LOCAL, REMOTE_WRITE, ON, REMOTE_APPLY
    }

    public SubscriptionOption(String name, Kind kind, Expression value) {
        super(name, kind, value);
    }

    public Boolean getBooleanValue() {
        switch (getKind()) {
            case CONNECT:
            case CREATE_SLOT:
            case ENABLED:
            case BINARY:
            case COPY_DATA:
            case TWO_PHASE:
            case DISABLE_ON_ERROR:
            case PASSWORD_REQUIRED:
            case RUN_AS_OWNER:
            case FAILOVER:
            case REFRESH:
                return booleanValue();
            default:
                return null;
        }
    }

    public Streaming getStreaming() {
        return getKind() == Kind.STREAMING ? enumValue(Streaming.class) : null;
    }

    public Origin getOrigin() {
        return getKind() == Kind.ORIGIN ? enumValue(Origin.class) : null;
    }

    public SynchronousCommit getSynchronousCommit() {
        return getKind() == Kind.SYNCHRONOUS_COMMIT ? enumValue(SynchronousCommit.class) : null;
    }

    /** Quoted 'NONE' remains a literal slot name, not the NONE keyword. */
    public boolean isSlotNameNone() {
        return getKind() == Kind.SLOT_NAME && !(getValue() instanceof StringValue)
                && "NONE".equalsIgnoreCase(getValueText());
    }
}
