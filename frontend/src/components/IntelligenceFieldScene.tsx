import React, { useEffect, useRef } from 'react';
import * as THREE from 'three';

interface IntelligenceFieldSceneProps {
  isSearching?: boolean;
  activePhase?: number; // 1: Discover, 2: Understand, 3: Match, 4: Prioritize, 5: Prepare
}

export const IntelligenceFieldScene: React.FC<IntelligenceFieldSceneProps> = ({
  isSearching = false,
  activePhase = 1,
}) => {
  const mountRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const container = mountRef.current;
    if (!container) return;

    // Check for prefers-reduced-motion
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Setup Scene, Camera, Renderer
    const scene = new THREE.Scene();
    const width = container.clientWidth || window.innerWidth;
    const height = container.clientHeight || 450;

    const camera = new THREE.PerspectiveCamera(50, width / height, 0.1, 1000);
    camera.position.z = 85;

    let renderer: THREE.WebGLRenderer;
    try {
      renderer = new THREE.WebGLRenderer({
        antialias: true,
        alpha: true,
        powerPreference: 'high-performance',
      });
    } catch (e) {
      console.warn('WebGL initialization failed, falling back to static visual', e);
      return;
    }

    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    container.appendChild(renderer.domElement);

    // Objects Group
    const mainGroup = new THREE.Group();
    scene.add(mainGroup);

    // 1. Central Candidate Node (Technical Core)
    const coreGeo = new THREE.OctahedronGeometry(2.5, 0);
    const coreMat = new THREE.MeshBasicMaterial({
      color: 0x00f59b,
      wireframe: true,
      transparent: true,
      opacity: 0.85,
    });
    const coreMesh = new THREE.Mesh(coreGeo, coreMat);
    mainGroup.add(coreMesh);

    // Inner Core Glow Point
    const innerGeo = new THREE.SphereGeometry(1.0, 16, 16);
    const innerMat = new THREE.MeshBasicMaterial({
      color: 0x00f59b,
      transparent: true,
      opacity: 0.9,
    });
    const innerMesh = new THREE.Mesh(innerGeo, innerMat);
    mainGroup.add(innerMesh);

    // 2. Orbital Rings (ATS Web Domains: Greenhouse, Lever, Ashby, Workday)
    const ringRadii = [14, 24, 34, 44];
    const ringMeshes: THREE.Line[] = [];
    const ringColors = [0x1f2937, 0x374151, 0x1e293b, 0x334155];

    ringRadii.forEach((radius, i) => {
      const ringGeo = new THREE.BufferGeometry();
      const points: THREE.Vector3[] = [];
      const segments = 64;
      for (let s = 0; s <= segments; s++) {
        const theta = (s / segments) * Math.PI * 2;
        points.push(new THREE.Vector3(Math.cos(theta) * radius, Math.sin(theta) * radius, 0));
      }
      ringGeo.setFromPoints(points);

      const ringMat = new THREE.LineBasicMaterial({
        color: ringColors[i % ringColors.length],
        transparent: true,
        opacity: 0.4,
      });

      const ring = new THREE.Line(ringGeo, ringMat);
      ring.rotation.x = Math.PI * 0.25 * (i % 2 === 0 ? 1 : -1);
      ring.rotation.y = Math.PI * 0.15 * i;
      mainGroup.add(ring);
      ringMeshes.push(ring);
    });

    // 3. Particle Field (Job Signals & Data Nodes)
    const particleCount = 120;
    const particlePositions = new Float32Array(particleCount * 3);
    const particleScales = new Float32Array(particleCount);

    for (let i = 0; i < particleCount; i++) {
      const radius = 10 + Math.random() * 45;
      const theta = Math.random() * Math.PI * 2;
      const phi = Math.acos(Math.random() * 2 - 1);

      particlePositions[i * 3] = radius * Math.sin(phi) * Math.cos(theta);
      particlePositions[i * 3 + 1] = radius * Math.sin(phi) * Math.sin(theta);
      particlePositions[i * 3 + 2] = radius * Math.cos(phi) * 0.5; // flatten depth slightly
      particleScales[i] = Math.random() * 2.0 + 1.0;
    }

    const particlesGeo = new THREE.BufferGeometry();
    particlesGeo.setAttribute('position', new THREE.BufferAttribute(particlePositions, 3));

    const particlesMat = new THREE.PointsMaterial({
      color: 0x9ba1ad,
      size: 1.2,
      transparent: true,
      opacity: 0.65,
    });

    const particleSystem = new THREE.Points(particlesGeo, particlesMat);
    mainGroup.add(particleSystem);

    // 4. Connecting Signal Lines (Match / Signal Streams)
    const lineCount = 8;
    const lineGeometries: THREE.BufferGeometry[] = [];
    const lineMeshes: THREE.Line[] = [];

    for (let i = 0; i < lineCount; i++) {
      const lineGeo = new THREE.BufferGeometry();
      const pIdx = Math.floor(Math.random() * particleCount);
      const targetPos = new THREE.Vector3(
        particlePositions[pIdx * 3],
        particlePositions[pIdx * 3 + 1],
        particlePositions[pIdx * 3 + 2]
      );

      lineGeo.setFromPoints([new THREE.Vector3(0, 0, 0), targetPos]);
      const lineMat = new THREE.LineBasicMaterial({
        color: 0x00f59b,
        transparent: true,
        opacity: 0.25,
      });

      const line = new THREE.Line(lineGeo, lineMat);
      mainGroup.add(line);
      lineGeometries.push(lineGeo);
      lineMeshes.push(line);
    }

    // Mouse Interaction
    let mouseX = 0;
    let mouseY = 0;
    let targetX = 0;
    let targetY = 0;

    const handleMouseMove = (event: MouseEvent) => {
      const rect = container.getBoundingClientRect();
      const x = event.clientX - rect.left - rect.width / 2;
      const y = event.clientY - rect.top - rect.height / 2;
      mouseX = (x / (rect.width / 2)) * 0.3;
      mouseY = (y / (rect.height / 2)) * 0.3;
    };

    window.addEventListener('mousemove', handleMouseMove);

    // Handle Resize
    const handleResize = () => {
      if (!container) return;
      const w = container.clientWidth;
      const h = container.clientHeight;
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h);
    };

    window.addEventListener('resize', handleResize);

    // Animation Loop
    let animationFrameId: number;
    let clock = new THREE.Clock();

    const animate = () => {
      animationFrameId = requestAnimationFrame(animate);

      if (!prefersReducedMotion) {
        const delta = clock.getDelta();
        const speed = isSearching ? 1.8 : 0.8;

        // Smooth camera dampening
        targetX += (mouseX - targetX) * 0.05;
        targetY += (mouseY - targetY) * 0.05;
        mainGroup.rotation.y = targetX * 0.8 + clock.getElapsedTime() * 0.06 * speed;
        mainGroup.rotation.x = -targetY * 0.5;

        // Pulse core
        const pulse = 1 + Math.sin(clock.getElapsedTime() * 2 * speed) * 0.08;
        coreMesh.scale.set(pulse, pulse, pulse);
        coreMesh.rotation.x += delta * 0.2 * speed;
        coreMesh.rotation.y += delta * 0.3 * speed;

        // Rotate Orbital Rings
        ringMeshes.forEach((ring, idx) => {
          ring.rotation.z += delta * 0.05 * (idx % 2 === 0 ? 1 : -1) * speed;
        });

        // Dynamic Accent based on active search / phase
        if (isSearching) {
          particlesMat.color.setHex(0x00f59b);
          particlesMat.size = 1.6;
        } else {
          particlesMat.color.setHex(0x9ba1ad);
          particlesMat.size = 1.2;
        }
      }

      renderer.render(scene, camera);
    };

    animate();

    // Cleanup
    return () => {
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('resize', handleResize);

      coreGeo.dispose();
      coreMat.dispose();
      innerGeo.dispose();
      innerMat.dispose();
      particlesGeo.dispose();
      particlesMat.dispose();

      ringMeshes.forEach((r) => {
        r.geometry.dispose();
        (r.material as THREE.Material).dispose();
      });

      lineGeometries.forEach((g) => g.dispose());
      lineMeshes.forEach((l) => (l.material as THREE.Material).dispose());

      if (renderer.domElement && container.contains(renderer.domElement)) {
        container.removeChild(renderer.domElement);
      }
      renderer.dispose();
    };
  }, [isSearching, activePhase]);

  return (
    <div className="relative w-full h-[380px] sm:h-[460px] flex items-center justify-center overflow-hidden pointer-events-none select-none">
      <div ref={mountRef} className="absolute inset-0 w-full h-full pointer-events-auto" />

      {/* Atmospheric vignette overlays */}
      <div className="absolute inset-0 bg-gradient-to-t from-dark-950 via-transparent to-dark-950/80 pointer-events-none" />
      <div className="absolute inset-0 bg-radial-subtle pointer-events-none" />

      {/* Technical HUD Overlay Labels */}
      <div className="absolute bottom-4 left-6 sm:left-10 font-mono text-[10px] text-ink-muted uppercase tracking-widest flex items-center gap-2 pointer-events-none">
        <span className="w-1.5 h-1.5 rounded-full bg-signal-emerald animate-pulse" />
        <span>INTELLIGENCE STREAM // ACTIVE</span>
      </div>

      <div className="absolute bottom-4 right-6 sm:right-10 font-mono text-[10px] text-ink-muted uppercase tracking-widest pointer-events-none hidden sm:block">
        <span>GRID LATENCY: 24MS · SECURE GATE</span>
      </div>
    </div>
  );
};
