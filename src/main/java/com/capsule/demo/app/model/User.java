package com.capsule.demo.app.model;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "user")
@Data
public class User implements Serializable {
    @Id
    @NotEmpty
    private String id;

    @Column(name = "nick_name")
    @NotEmpty
    private String nickName;

    @Column(name = "utc_create")
    @Temporal(TemporalType.TIMESTAMP)
    private Date utcCreate;

    @Column(name = "utc_modified")
    @Temporal(TemporalType.TIMESTAMP)
    private Date utcModified;
}
