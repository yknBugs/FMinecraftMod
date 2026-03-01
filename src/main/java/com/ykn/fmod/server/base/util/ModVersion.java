package com.ykn.fmod.server.base.util;

import java.util.Objects;

/**
 * Represents an immutable version identifier for the mod, following the format
 * {@code major.release[.push][+b&lt;build&gt;]}.
 *
 * <p>Examples of valid version strings:
 * <ul>
 *   <li>{@code 1.0} – stable release, push defaults to 0</li>
 *   <li>{@code 1.2.3} – latest push with push number</li>
 *   <li>{@code 1.2.3+b4} – unstable build, only exists in development</li>
 * </ul>
 *
 * <p>Version ordering: a stable version is always considered newer than an
 * unstable version with the same {@code major}, {@code release} and {@code push}
 * numbers. Among unstable versions, higher {@code build} numbers are newer.
 */
public final class ModVersion implements Comparable<ModVersion> {

    /**
     * The major version component (the {@code 1} in {@code 1.x}).
     * Starts from {@code 0}.
     */
    public final int major;

    /**
     * The release version component (the {@code x} in {@code 1.x}).
     * Starts from {@code 0}.
     */
    public final int release;

    /**
     * The push version component (the {@code x} in {@code x.x.1}).
     * Starts from {@code 1}; {@code 0} indicates it is omitted in the string
     * representation.
     */
    public final int push;

    /**
     * Whether this version is a stable release.
     * {@code true} means there is no {@code +b&lt;build&gt;} suffix.
     */
    public final boolean stable;

    /**
     * The build number for unstable versions (the {@code 1} in {@code +b1}).
     * Starts from {@code 1}; {@code 0} when {@link #stable} is {@code true}.
     */
    public final int build;

    /**
     * Constructs a new {@code ModVersion} with all components explicitly specified.
     *
     * @param major   the major version number (≥ 0)
     * @param release the release version number (≥ 0)
     * @param push    the push version number (0 to omit from string output)
     * @param stable  {@code true} if this is a stable release
     * @param build   the build number for unstable versions (ignored when {@code stable} is {@code true})
     */
    public ModVersion(int major, int release, int push, boolean stable, int build) {
        this.major = major;
        this.release = release;
        this.push = push;
        this.stable = stable;
        this.build = build;
    }

    /**
     * Parses a version string into a {@code ModVersion} instance.
     *
     * <p>Accepted formats:
     * <ul>
     *   <li>{@code major.release}</li>
     *   <li>{@code major.release.push}</li>
     *   <li>{@code major.release.push+b&lt;build&gt;}</li>
     * </ul>
     *
     * @param version the version string to parse; must not be {@code null}
     * @return a new {@code ModVersion} representing the parsed version
     * @throws IllegalArgumentException if the version string does not conform to the expected format
     * @throws NumberFormatException    if any numeric component cannot be parsed
     */
    public static ModVersion fromString(String version) {
        String[] buildSplit = version.split("\\+b");

        // parse build
        boolean stable = buildSplit.length == 1;
        int build = 0;

        if (buildSplit.length > 2) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }

        if (!stable) {
            build = Integer.parseInt(buildSplit[1]);
        }

        String[] mainParts = buildSplit[0].split("\\.");
        if (mainParts.length < 2 || mainParts.length > 3) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }

        int major = Integer.parseInt(mainParts[0]);
        int release = Integer.parseInt(mainParts[1]);
        int push = mainParts.length == 3 ? Integer.parseInt(mainParts[2]) : 0;

        if (build < 0 || major < 0 || release < 0 || push < 0) {
            throw new IllegalArgumentException("Version components must be non-negative: " + version);
        }

        return new ModVersion(major, release, push, stable, build);
    }

    /**
     * Returns the canonical string representation of this version.
     *
     * <p>The format is {@code major.release[.push][+b&lt;build&gt;]}, where the
     * {@code .push} segment is omitted when {@link #push} is {@code 0}, and the
     * {@code +b&lt;build&gt;} suffix is omitted for stable versions.
     *
     * @return the version string; never {@code null}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(major).append('.').append(release);
        if (push > 0) {
            sb.append('.').append(push);
        }
        if (!stable) {
            sb.append("+b").append(build);
        }
        return sb.toString();
    }

    /**
     * Compares this version to another version for ordering.
     *
     * <p>Components are compared in the following order:
     * {@link #major}, {@link #release}, {@link #push}, stability (stable &gt; unstable),
     * and finally {@link #build} number for unstable versions.
     *
     * @param other the version to compare against; must not be {@code null}
     * @return a negative integer, zero, or a positive integer if this version is
     *         less than, equal to, or greater than {@code other}, respectively
     */
    @Override
    public int compareTo(ModVersion other) {
        if (this.major != other.major)
            return Integer.compare(this.major, other.major);

        if (this.release != other.release)
            return Integer.compare(this.release, other.release);

        if (this.push != other.push)
            return Integer.compare(this.push, other.push);

        // Stable (no +b) is newer than unstable
        if (this.stable != other.stable)
            return this.stable ? 1 : -1;

        // If both unstable, compare build
        return Integer.compare(this.build, other.build);
    }

    /**
     * Indicates whether some other object is equal to this version.
     *
     * <p>Two {@code ModVersion} instances are equal if and only if all five
     * components ({@link #major}, {@link #release}, {@link #push}, {@link #stable},
     * {@link #build}) are identical.
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if {@code obj} is a {@code ModVersion} with the same
     *         component values; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ModVersion)) {
            return false;
        }
        ModVersion other = (ModVersion) obj;
        return this.major == other.major
                && this.release == other.release
                && this.push == other.push
                && this.stable == other.stable
                && this.build == other.build;
    }

    /**
     * Returns a hash code value consistent with {@link #equals(Object)}.
     *
     * @return the hash code computed from all five version components
     */
    @Override
    public int hashCode() {
        return Objects.hash(major, release, push, stable, build);
    }

    /**
     * Compares two version strings lexicographically according to the version
     * ordering defined by {@link #compareTo(ModVersion)}.
     *
     * @param v1 the first version string; must not be {@code null}
     * @param v2 the second version string; must not be {@code null}
     * @return a negative integer, zero, or a positive integer if {@code v1} is
     *         less than, equal to, or greater than {@code v2}
     * @throws IllegalArgumentException if either string is not a valid version
     */
    public static int compare(String v1, String v2) {
        return fromString(v1).compareTo(fromString(v2));
    }

    /**
     * Compares two {@code ModVersion} instances according to the version ordering
     * defined by {@link #compareTo(ModVersion)}.
     *
     * @param v1 the first version; must not be {@code null}
     * @param v2 the second version; must not be {@code null}
     * @return a negative integer, zero, or a positive integer if {@code v1} is
     *         less than, equal to, or greater than {@code v2}
     */
    public static int compare(ModVersion v1, ModVersion v2) {
        return v1.compareTo(v2);
    }
    
}
