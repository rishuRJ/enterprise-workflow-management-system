package com.rishu.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ProcessedEventId implements Serializable {
    private Long requestId;
    private String action;
}
