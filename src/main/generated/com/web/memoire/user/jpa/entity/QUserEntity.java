package com.web.memoire.user.jpa.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserEntity is a Querydsl query type for UserEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserEntity extends EntityPathBase<UserEntity> {

    private static final long serialVersionUID = -1249877034L;

    public static final QUserEntity userEntity = new QUserEntity("userEntity");

    public final StringPath autoLoginFlag = createString("autoLoginFlag");

    public final StringPath autoLoginToken = createString("autoLoginToken");

    public final DateTimePath<java.util.Date> birthday = createDateTime("birthday", java.util.Date.class);

    public final StringPath faceLoginUse = createString("faceLoginUse");

    public final StringPath loginId = createString("loginId");

    public final StringPath name = createString("name");

    public final StringPath nickname = createString("nickname");

    public final StringPath password = createString("password");

    public final StringPath phone = createString("phone");

    public final StringPath profileImagePath = createString("profileImagePath");

    public final DateTimePath<java.util.Date> registrationDate = createDateTime("registrationDate", java.util.Date.class);

    public final StringPath role = createString("role");

    public final NumberPath<Integer> sanctionCount = createNumber("sanctionCount", Integer.class);

    public final StringPath statusMessage = createString("statusMessage");

    public final StringPath userId = createString("userId");

    public QUserEntity(String variable) {
        super(UserEntity.class, forVariable(variable));
    }

    public QUserEntity(Path<? extends UserEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserEntity(PathMetadata metadata) {
        super(UserEntity.class, metadata);
    }

}

